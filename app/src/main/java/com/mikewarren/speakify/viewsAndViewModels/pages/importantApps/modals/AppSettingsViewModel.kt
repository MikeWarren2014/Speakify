package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.UserAppModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppSettingsViewModel(
    val appModel: UserAppModel,
    val initialSettingsModel: AppSettingsModel,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    var isOpen by mutableStateOf(false)

    private val _settings = MutableStateFlow(initialSettingsModel)
    val settings: StateFlow<AppSettingsModel> = _settings.asStateFlow()

    // extract notificationSources,announcerVoice from settingsRepository.appSettings
    private var notificationSources = initialSettingsModel.notificationSources
    private var announcerVoice = initialSettingsModel.announcerVoice

    init {
        viewModelScope.launch {
            settingsRepository.appSettings.collect { appSettings: Map<String, AppSettingsModel> ->
                val appSettingsModel = appSettings[appModel.packageName]

                notificationSources = appSettingsModel?.notificationSources ?: initialSettingsModel.notificationSources
                announcerVoice = appSettingsModel?.announcerVoice ?: initialSettingsModel.announcerVoice
            }
        }
    }

    val childAnnouncerVoiceSectionViewModel = AnnouncerVoiceSectionViewModel(
        settingsRepository = settingsRepository,
        initialVoice = announcerVoice ?: "",
        onSave = { voiceName: String ->
            _settings.update { model: AppSettingsModel ->
                model.copy(announcerVoice = voiceName)
            }
        }
    )

    val childNotificationListViewModel = createNotificationSourceListViewModel()

    fun getPackageName(): String {
        return appModel.packageName
    }

    fun open() {
        isOpen = true
        childAnnouncerVoiceSectionViewModel.onOpen()
        childNotificationListViewModel?.onOpen()
    }

    fun createNotificationSourceListViewModel(): BaseNotificationSourceListViewModel<*>? {
        // TODO: should we have a separate view model for each app?
        if ((getPackageName() in Constants.PhoneAppPackageNames) ||
            (getPackageName() in Constants.MessagingAppPackageNames))
            return BaseImportantContactsListViewModel(settingsRepository,
                notificationSources,
                { importantContacts: List<String> ->
                    _settings.update { model: AppSettingsModel ->
                        model.copy(notificationSources = importantContacts)
                    }
                },
            )

        return null
    }

    fun cancel() {
        childAnnouncerVoiceSectionViewModel.cancel()
        childNotificationListViewModel?.cancel()

    }

    fun save() {
        childAnnouncerVoiceSectionViewModel.onSave()
        childNotificationListViewModel?.onSave()
        viewModelScope.launch {
            settingsRepository.saveAppSettings(settings.value)
        }
    }
}