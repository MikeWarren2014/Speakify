package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.EmailContactModel
import com.mikewarren.speakify.data.NotificationSource
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.events.EmailContactListDataRequester
import com.mikewarren.speakify.viewsAndViewModels.widgets.UiText
import kotlinx.coroutines.flow.StateFlow

@NotificationListComponent(appTypes = ["email"])
class EmailImportantContactsListViewModel(
    override var settingsRepository: SettingsRepository,
    selectedNotificationSources: List<NotificationSource>,
    onSave: (List<NotificationSource>) -> Any,
) : BaseAutoCompletableNotificationSourceListViewModel<EmailContactModel>(
    settingsRepository,
    selectedNotificationSources,
    onSave,
) {

    private val dataSource = EmailContactListDataRequester.GetInstance(settingsRepository.getContext())

    override val allData: StateFlow<List<EmailContactModel>> = dataSource.observeData()

    fun fetchRecentContacts() {
        dataSource.requestData()
    }

    override fun onOpen() {
        super.onOpen()
        fetchRecentContacts()
    }

    override fun getNotificationSourcesNameText(): UiText {
        return UiText.StringResource(R.string.email_contacts_text)
    }

    override fun toSourceString(value: EmailContactModel): String {
        return value.email.ifBlank { value.name }
    }

    override fun toNotificationSource(sourceModel: EmailContactModel): NotificationSource {
        val targetValue = sourceModel.email.ifBlank { sourceModel.name }
        return NotificationSource(
            value = targetValue,
            name = sourceModel.name
        )
    }

    override fun toViewString(value: EmailContactModel): String {
        if (value.name.isBlank())
            return value.email

        if ((value.email.isBlank()) || (value.email.equals(value.name, ignoreCase = true)))
            return value.name

        return "${value.name} (${value.email})"
    }

    override fun getLabelText(): UiText {
        return UiText.StringResource(R.string.autocomplete_label_contact_name)
    }

    override fun getAllChoices(): List<EmailContactModel> {
        return allAddableSourceModels.value
    }
}
