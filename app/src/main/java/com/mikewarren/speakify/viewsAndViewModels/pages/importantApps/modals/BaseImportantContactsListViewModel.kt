package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import com.mikewarren.speakify.data.ContactModel
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.events.ContactListDataSource
import kotlinx.coroutines.flow.StateFlow

class BaseImportantContactsListViewModel(
    override var settingsRepository: SettingsRepository,
    notificationSourceList: List<String>,
    onSave: (List<String>) -> Any,
) : BaseNotificationSourceListViewModel<ContactModel>(settingsRepository,
    notificationSourceList,
    onSave,
) {
    protected val dataSource = ContactListDataSource(settingsRepository.getContext())

    override val allData: StateFlow<List<ContactModel>> = dataSource.observeContacts()

    fun fetchContacts() {
        dataSource.requestContacts()
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
}