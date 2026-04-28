package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.ContactModel
import com.mikewarren.speakify.data.NotificationSource
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.viewsAndViewModels.widgets.IStringConverter
import com.mikewarren.speakify.viewsAndViewModels.widgets.UiText
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PhoneImportantContactsListViewModel(
    override var settingsRepository: SettingsRepository,
    override var selectedNotificationSources: List<NotificationSource>,
    override val onSave: (List<NotificationSource>) -> Any,
) : ViewModel(), INotificationSourceListViewModel<ContactModel>, IStringConverter<ContactModel> {

    protected val _notificationSourcesFlow = MutableStateFlow(selectedNotificationSources)
    override val notificationSourcesFlow: StateFlow<List<NotificationSource>> = _notificationSourcesFlow.asStateFlow()

    private val _launchContactPickerEvent = MutableSharedFlow<Unit>(replay = 0)
    val launchContactPickerEvent = _launchContactPickerEvent.asSharedFlow()

    override fun isLoading(): Boolean {
        return notificationSourcesFlow.value
            .isEmpty()
    }

    override fun getMainDataStream(): StateFlow<List<ContactModel>>? {
        // We don't need a main data stream
        return null
    }

    override fun getAddedSourceModels(): List<ContactModel> {
        return notificationSourcesFlow.value
            .map { ContactModel(it.name.toString(), it.value) }
    }


    override fun getNotificationSourcesNameText(): UiText {
        return UiText.StringResource(R.string.contacts_name_text)
    }

    override fun toSourceString(value: ContactModel): String {
        return value.phoneNumber
    }

    override fun toViewString(value: ContactModel): String {
        if (value.name.isEmpty())
            return value.phoneNumber

        return "${value.name} (${value.phoneNumber})"
    }

    override fun toNotificationSource(sourceModel: ContactModel): NotificationSource {
        return NotificationSource(sourceModel.phoneNumber, sourceModel.name)
    }


    override fun setNotificationSources(notificationSources: List<NotificationSource>) {
        _notificationSourcesFlow.value = notificationSources
    }

    fun onAddContactClicked() {
        viewModelScope.launch {
            _launchContactPickerEvent.emit(Unit)
        }
    }

    override fun onOpen() {
        // I don't think we need to do anything on open...
        // We no longer fetch contacts here, and the data is being passed directly to this view model from the parent view model.
        // So we stub this out.
    }

}
