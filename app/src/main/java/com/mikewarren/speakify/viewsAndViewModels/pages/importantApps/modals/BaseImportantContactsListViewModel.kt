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
        if (sourceModel.name.isEmpty())
            return sourceModel.phoneNumber

        return "${sourceModel.name} (${sourceModel.phoneNumber})"
    }

    override fun getLabel(): String {
        return "Contact Name/Phone Number"
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
