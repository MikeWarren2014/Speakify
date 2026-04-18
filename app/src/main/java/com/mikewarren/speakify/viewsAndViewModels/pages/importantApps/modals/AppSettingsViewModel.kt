package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.NotificationSource
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.constants.PackageNames
import com.mikewarren.speakify.data.db.UserAppModel
import com.mikewarren.speakify.services.TTSManager
import com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.widgets.BaseAppAdditionalSettingsViewModel
import com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.widgets.BaseMessagingAppAdditionalSettingsViewModel
import com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.widgets.MessengerAdditionalSettingsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppSettingsViewModel(
    val appModel: UserAppModel,
    val initialSettingsModel: AppSettingsModel,
    private val settingsRepository: SettingsRepository,
    private val ttsManager: TTSManager,
) : ViewModel() {
    var isOpen by mutableStateOf(false)

    val modelFlow : StateFlow<AppSettingsModel?> = settingsRepository.appSettings
        .distinctUntilChanged()
        .map { appSettings: Map<String, AppSettingsModel> ->
            return@map appSettings[appModel.packageName]
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = initialSettingsModel
        )

    private val _settings = MutableStateFlow(initialSettingsModel)
    val settings: StateFlow<AppSettingsModel> = _settings.asStateFlow()

    var childAnnouncerVoiceSectionViewModel: AnnouncerVoiceSectionViewModel? = null
    var childNotificationListViewModel: BaseNotificationSourceListViewModel<*>? = null
    var childAdditionalSettingsViewModel: BaseAppAdditionalSettingsViewModel? = null

    fun getPackageName(): String {
        return appModel.packageName
    }

    init {
        viewModelScope.launch {
            modelFlow.collectLatest { model: AppSettingsModel? ->
                if (model == null) {
                    return@collectLatest
                }
                _settings.value = model


                childAnnouncerVoiceSectionViewModel = AnnouncerVoiceSectionViewModel(
                    settingsRepository,
                    ttsManager,
                    initialVoice = model.announcerVoice ?: Constants.DefaultTTSVoice,
                    onSave = { voiceName: String ->
                        _settings.update { model: AppSettingsModel ->
                            model.copy(announcerVoice = voiceName)
                        }
                    }
                )
                childNotificationListViewModel = createNotificationSourceListViewModel(model)
                childAdditionalSettingsViewModel = createAdditionalSettingsViewModel(model)
            }
        }
    }

    fun open() {
        isOpen = true

        childAnnouncerVoiceSectionViewModel?.onOpen()
        childNotificationListViewModel?.onOpen()
        childAdditionalSettingsViewModel?.onOpen()
    }

    fun createNotificationSourceListViewModel(model: AppSettingsModel): BaseNotificationSourceListViewModel<*>? {
        Log.d("AppSettingsViewModel", "packageName = '${appModel.packageName}' , notificationSources = ${model.notificationSources}")

        if ((getPackageName() in PackageNames.PhoneAppList) ||
            (getPackageName() in PackageNames.MessagingAppList) ||
            (getPackageName() == PackageNames.GoogleVoice)
        )
            return BaseImportantContactsListViewModel(
                settingsRepository,
                model.notificationSources,
                { importantContacts: List<NotificationSource> ->
                    _settings.update { model: AppSettingsModel ->
                        model.copy(notificationSources = importantContacts)
                    }
                },
            )

        if (PackageNames.FacebookMessengerAppList.contains(getPackageName())) {
            return MessengerImportantContactsListViewModel(
                settingsRepository,
                model.notificationSources,
                { importantContacts: List<NotificationSource> ->
                    _settings.update { model: AppSettingsModel ->
                        model.copy(notificationSources = importantContacts)
                    }
                },
            )
        }

        return null
    }

    private fun createAdditionalSettingsViewModel(model: AppSettingsModel): BaseAppAdditionalSettingsViewModel? {
        if (getPackageName() in PackageNames.MessagingAppList) {
            return BaseMessagingAppAdditionalSettingsViewModel(
                settingsRepository,
                model.additionalSettings,
                onSaveSettings = { additionalSettings: Map<String, String> ->
                    _settings.update { model: AppSettingsModel ->
                        model.copy(additionalSettings = additionalSettings)
                    }
                }
            )
        }
        if (PackageNames.FacebookMessengerAppList.contains(getPackageName())) {
            return MessengerAdditionalSettingsViewModel(
                settingsRepository,
                model.additionalSettings,
                onSaveSettings = { additionalSettings: Map<String, String> ->
                    _settings.update { model: AppSettingsModel ->
                        model.copy(additionalSettings = additionalSettings)
                    }
                }
            )
        }
        return null
    }

    fun cancel() {
        childAnnouncerVoiceSectionViewModel?.cancel()
        childNotificationListViewModel?.cancel()
        childAdditionalSettingsViewModel?.cancel()
    }

    fun save() {
        childAnnouncerVoiceSectionViewModel?.onSave()
        childNotificationListViewModel?.onSave()
        childAdditionalSettingsViewModel?.onSave()
        viewModelScope.launch {
            settingsRepository.saveAppSettings(settings.value)
            Log.d("AppSettingsViewModel", "Saved settings: ${settings.value}")
        }
    }
}