package com.mikewarren.speakify.data.models.scheduling

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.viewsAndViewModels.widgets.UiText

class StatusSectionViewModel(
    val initialStatus: StatusModel,
    val onSave: (StatusModel) -> Any,
): ViewModel() {
    val originalStatus = initialStatus

    var isAppOn by mutableStateOf(initialStatus is StatusModel.On)
    var pauseHours by mutableStateOf("")
    var pauseMinutes by mutableStateOf("")

    var errorsDict: Map<String, List<UiText>> by mutableStateOf(emptyMap())
        private set

    var isOpen by mutableStateOf(false)

    init {
        setFromOriginalStatus()
    }

    private fun setFromOriginalStatus() {
        isAppOn = originalStatus is StatusModel.On
        if (originalStatus is StatusModel.On) {
            return
        }
        val turnOnTime = (originalStatus as StatusModel.Off).turnOnTime
        if (turnOnTime != null) {
            val hours = (turnOnTime / Constants.OneHour).toInt()
            val minutes = ((turnOnTime % Constants.OneHour) / Constants.OneMinute).toInt()
            pauseHours = hours.toString()
            pauseMinutes = minutes.toString()

        }
    }

    fun toggleAppStatus() {
        isAppOn = !isAppOn
        if (isAppOn) {
            save()
            return
        }
        isOpen = true
    }

    fun updatePauseHours(newHours: String) {
        pauseHours = newHours
        validate()
    }

    fun updatePauseMinutes(newMinutes: String) {
        pauseMinutes = newMinutes
        validate()
    }

    fun validate(): Boolean {
        val newErrors = mutableMapOf<String, List<UiText>>()

        val hourErrors = validateField(pauseHours)
        if (hourErrors.isNotEmpty()) {
            newErrors[HoursField] = hourErrors
        }

        val minuteErrors = validateField( pauseMinutes)
        if (minuteErrors.isNotEmpty()) {
            newErrors[MinutesField] = minuteErrors
        }

        if ((pauseHours.isEmpty()) && (pauseMinutes.isEmpty())) {
            newErrors[BothFields] = listOf(UiText.StringResource(R.string.scheduling_pause_duration_error_both_fields))
        }

        errorsDict = newErrors
        return newErrors.isEmpty()
    }

    fun validateField(fieldValue: String): List<UiText> {
        val fieldErrors = mutableListOf<UiText>()

        if ("""[^\d+]""".toRegex().containsMatchIn(fieldValue)) {
            fieldErrors.add(UiText.StringResource(R.string.scheduling_pause_duration_error_non_digit_characters))
        }

        if (fieldValue.length > 3) {
            fieldErrors.add(UiText.StringResource(R.string.scheduling_pause_duration_error_hours_too_long))
        }

        return fieldErrors
    }

    fun save() {
        if (!validate()) {
            return
        }

        val newStatus = if (isAppOn) StatusModel.On else
            StatusModel.Off(calculateTurnOnTime())
        onSave(newStatus)

        isOpen = false
    }

    fun calculateTurnOnTime(): Long {
        if (isAppOn) {
            return System.currentTimeMillis()
        }

        val pauseHoursInt = pauseHours.toIntOrNull() ?: 0
        val pauseMinutesInt = pauseMinutes.toIntOrNull() ?: 0

        return System.currentTimeMillis() + (pauseHoursInt * Constants.OneHour + pauseMinutesInt * Constants.OneMinute)
    }

    fun cancel() {
        setFromOriginalStatus()
        isOpen = false
    }

    fun open() {
        isOpen = true
    }

    companion object {
        const val HoursField = "hoursField"
        const val MinutesField = "minutesField"
        const val BothFields = "bothFields"
    }
}