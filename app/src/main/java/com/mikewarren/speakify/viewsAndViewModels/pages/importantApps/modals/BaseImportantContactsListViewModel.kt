package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import com.mikewarren.speakify.data.ContactModel
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.events.ContactListDataSource
import com.mikewarren.speakify.utils.PhoneNumberUtils
import kotlinx.coroutines.flow.StateFlow

class BaseImportantContactsListViewModel(
    override var settingsRepository: SettingsRepository,
    notificationSourceList: List<String>,
    onSave: (List<String>) -> Any,
) : BaseNotificationSourceListViewModel<ContactModel>(settingsRepository,
    notificationSourceList,
    onSave,
) {

    protected val dataSource = ContactListDataSource.GetInstance(settingsRepository.getContext())

    override val allData: StateFlow<List<ContactModel>> = dataSource.observeData()

    fun fetchContacts() {
        dataSource.requestData()
    }

    override fun getNotificationSourcesName(): String {
        return "contacts"
    }

    override fun toSourceString(sourceModel: ContactModel): String {
        return sourceModel.phoneNumber
    }

    override fun toViewString(sourceModel: ContactModel): String {
        return "${sourceModel.name} (${sourceModel.phoneNumber})"
    }

    override fun getLabel(): String {
        return "Contact Name/Phone Number"
    }

    override fun getAllChoices(): List<String> {
        if (allData.value.isEmpty())
            return emptyList()

        return allAddableSourceModels.value
            ?.filter { model: ContactModel -> model.phoneNumber.isNotEmpty() }
            ?.map { model: ContactModel ->
            if (model.name.isNotEmpty())
                return@map "${model.name} (${model.phoneNumber})"

            return@map model.phoneNumber
        } ?: emptyList()
    }

    override fun filterChoices(searchText: String): List<String> {
        if (("\\d+".toRegex().matches(searchText)) ||
            (PHONE_NUMBER_REGEX.toRegex().matches(searchText))) {
            return getAllChoices().filter { choice: String ->
                return@filter PhoneNumberUtils.ExtractOnlyDigits(extractPhoneNumberFromChoice(choice))
                    .contains(PhoneNumberUtils.ExtractOnlyDigits(searchText))
            }
        }
        return super.filterChoices(searchText)

    }

    private fun extractPhoneNumberFromChoice(choice: String): String {
        if (choice.last() == ')') {
            val result : MatchResult? = """(.*(?<=[^\d+]|[A-Za-z\d+]) )(?<phone>\(${PHONE_NUMBER_REGEX}\))"""
                .toRegex()
                .find(choice)
            if (result == null)
                throw IllegalStateException("Somehow we have a problem with the regex, as '${choice}' doesn't seem to match.")
            val matchGroup: MatchGroup? = result.groups["phone"]
            if (matchGroup == null)
                throw IllegalStateException("Somehow we cannot retrieve the phone number from the string, as '${choice}' doesn't seem to match")

            return matchGroup.value
        }
        return choice
    }

    companion object {
        const val PHONE_NUMBER_REGEX = """[#*+\d()\- ;,px]+"""
    }
}
