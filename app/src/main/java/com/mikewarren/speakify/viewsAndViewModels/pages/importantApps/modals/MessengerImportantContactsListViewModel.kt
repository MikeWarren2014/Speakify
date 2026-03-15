package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.MessengerContactModel
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.events.MessengerContactListDataRequester
import com.mikewarren.speakify.viewsAndViewModels.widgets.UiText
import kotlinx.coroutines.flow.StateFlow

class MessengerImportantContactsListViewModel(
    override var settingsRepository: SettingsRepository,
    notificationSourceList: List<String>,
    onSave: (List<String>) -> Any,
) : BaseNotificationSourceListViewModel<MessengerContactModel>(
    settingsRepository,
    notificationSourceList,
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

    override fun toSourceString(sourceModel: MessengerContactModel): String {
        return sourceModel.name
    }

    override fun toViewString(sourceModel: MessengerContactModel): String {
        return sourceModel.name
    }

    override fun getLabelText(): UiText {
        return UiText.StringResource(R.string.autocomplete_label_contact_name)
    }

    override fun getAllChoices(): List<MessengerContactModel> {
        return allAddableSourceModels.value
    }
}
