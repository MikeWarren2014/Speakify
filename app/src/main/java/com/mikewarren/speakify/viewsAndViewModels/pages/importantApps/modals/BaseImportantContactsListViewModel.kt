package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.ContactModel
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.events.ContactListDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class BaseImportantContactsListViewModel(
    override var settingsRepository: SettingsRepository,
    selectedNotificationSources: List<String>,
    onSave: (List<String>) -> Any,
) : BaseNotificationSourceListViewModel<ContactModel>(settingsRepository,
    selectedNotificationSources,
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