package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.widgets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.constants.appSettingsKeys.EmailAppKeys
import com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.AdditionalSettingsComponent

@AdditionalSettingsComponent(appTypes = ["email"])
class EmailAppAdditionalSettingsViewModel(
    settingsRepository: SettingsRepository,
    initialAdditionalSettings: Map<String, String>,
    onSaveSettings: (Map<String, String>) -> Unit,
) : BaseMessageReadingAdditionalSettingsViewModel(
    settingsRepository,
    initialAdditionalSettings,
    onSaveSettings
) {
    var announceMultipleMessages by mutableStateOf(
        initialAdditionalSettings[EmailAppKeys.KEY_ANNOUNCE_MULTIPLE_MESSAGE]?.toBoolean() ?: Constants.DefaultBooleanSetting
    )

    var keywordInput by mutableStateOf("")
        private set

    val keywordsList = mutableStateListOf<String>().apply {
        addAll(parseKeywordsString(initialAdditionalSettings[EmailAppKeys.KEY_MESSAGE_KEYWORDS] ?: ""))
    }

    private var originalAnnounceMultipleMessages = announceMultipleMessages
    private val originalKeywordsList = keywordsList.toList()

    fun onKeywordInputChange(newValue: String) {
        if ("," !in newValue) {
            keywordInput = newValue
            return
        }
        val parts = newValue.split(",")
        for (i in 0 until parts.size - 1) {
            addKeyword(parts[i])
        }
        keywordInput = parts.last().trimStart()
    }

    fun commitCurrentInput() {
        if (keywordInput.isNotBlank()) {
            addKeyword(keywordInput)
            keywordInput = ""
        }
    }

    fun addKeyword(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isNotEmpty() && !keywordsList.contains(trimmed)) {
            keywordsList.add(trimmed)
        }
    }

    fun removeKeyword(keyword: String) {
        keywordsList.remove(keyword)
    }

    override fun cancel() {
        super.cancel()
        announceMultipleMessages = originalAnnounceMultipleMessages
        keywordInput = ""
        keywordsList.clear()
        keywordsList.addAll(originalKeywordsList)
    }

    override fun makeAdditionalSettingsDict(): Map<String, String> {
        val baseDict = super.makeAdditionalSettingsDict().toMutableMap()
        baseDict[EmailAppKeys.KEY_ANNOUNCE_MULTIPLE_MESSAGE] = announceMultipleMessages.toString()

        val allKeywords = keywordsList.toMutableList()
        if (keywordInput.isNotBlank() && !allKeywords.contains(keywordInput.trim())) {
            allKeywords.add(keywordInput.trim())
        }

        baseDict[EmailAppKeys.KEY_MESSAGE_KEYWORDS] = allKeywords.joinToString(", ")
        return baseDict
    }

    private fun parseKeywordsString(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
