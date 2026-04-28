package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.MessengerContactModel
import com.mikewarren.speakify.data.NotificationSource
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.events.MessengerContactListDataRequester
import com.mikewarren.speakify.viewsAndViewModels.widgets.UiText
import kotlinx.coroutines.flow.StateFlow

class MessengerImportantContactsListViewModel(
    override var settingsRepository: SettingsRepository,
    selectedNotificationSources: List<NotificationSource>,
    onSave: (List<NotificationSource>) -> Any,
) : BaseAutoCompletableNotificationSourceListViewModel<MessengerContactModel>(
    settingsRepository,
    selectedNotificationSources,
    onSave,
) {

    private val dataSource = MessengerContactListDataRequester.GetInstance(settingsRepository.getContext())

    override val allData: StateFlow<List<MessengerContactModel>> = dataSource.observeData()

    fun fetchRecentContacts() {
        dataSource.requestData()
    }

    override fun onOpen() {
        super.onOpen()
        fetchRecentContacts()
    }

    override fun getNotificationSourcesNameText(): UiText {
        return UiText.StringResource(R.string.contacts_name_text)
    }

    override fun toSourceString(value: MessengerContactModel): String {
        return value.name
    }

    override fun toNotificationSource(sourceModel: MessengerContactModel): NotificationSource {
        return NotificationSource(
            value = sourceModel.name,
            name = sourceModel.name
        )
    }

    override fun toViewString(value: MessengerContactModel): String {
        return value.name
    }

    override fun getLabelText(): UiText {
        return UiText.StringResource(R.string.autocomplete_label_contact_name)
    }

    override fun getAllChoices(): List<MessengerContactModel> {
        return allAddableSourceModels.value
    }
}
