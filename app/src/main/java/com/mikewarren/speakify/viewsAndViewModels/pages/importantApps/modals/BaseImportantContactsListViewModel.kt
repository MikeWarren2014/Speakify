package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.ContactModel
import com.mikewarren.speakify.data.NotificationSource
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.events.ContactListDataRequester
import com.mikewarren.speakify.utils.PhoneNumberUtils
import com.mikewarren.speakify.viewsAndViewModels.widgets.UiText
import kotlinx.coroutines.flow.StateFlow

class BaseImportantContactsListViewModel(
    override var settingsRepository: SettingsRepository,
    notificationSourceList: List<NotificationSource>,
    onSave: (List<NotificationSource>) -> Any,
) : BaseNotificationSourceListViewModel<ContactModel>(settingsRepository,
    notificationSourceList,
    onSave,
) {

    protected val dataRequester = ContactListDataRequester.GetInstance(settingsRepository.getContext())

    override val allData: StateFlow<List<ContactModel>> = dataRequester.observeData()

    fun fetchContacts() {
        dataRequester.requestData()
    }

    override fun onOpen() {
        super.onOpen()
        fetchContacts()
    }

    override fun getNotificationSourcesNameText(): UiText {
        return UiText.StringResource(R.string.contacts_name_text)
    }

    override fun toSourceString(sourceModel: ContactModel): String {
        return sourceModel.phoneNumber
    }

    override fun toNotificationSource(sourceModel: ContactModel): NotificationSource {
        return NotificationSource(sourceModel.phoneNumber, sourceModel.name)
    }

    override fun toViewString(sourceModel: ContactModel): String {
        if (sourceModel.name.isEmpty())
            return sourceModel.phoneNumber

        return "${sourceModel.name} (${sourceModel.phoneNumber})"
    }

    override fun getLabelText(): UiText {
        return UiText.StringResource(R.string.autocomplete_label_contact_name_phone_number)
    }

    override fun getAllChoices(): List<ContactModel> {
        if (allData.value.isEmpty())
            return emptyList()

        return allAddableSourceModels.value
            .filter { model: ContactModel -> model.phoneNumber.isNotEmpty() }
    }

    override fun filterChoices(searchText: String): List<ContactModel> {
        if (("\\d+".toRegex().matches(searchText)) ||
            (PHONE_NUMBER_REGEX.toRegex().matches(searchText))) {
            return getAllChoices().filter { choice: ContactModel ->
                return@filter PhoneNumberUtils.ExtractOnlyDigits(choice.phoneNumber)
                    .contains(PhoneNumberUtils.ExtractOnlyDigits(searchText))
            }
        }
        return super.filterChoices(searchText)

    }

    companion object {
        const val PHONE_NUMBER_REGEX = """[#*+\d()\- ;,px]+"""
    }
}
