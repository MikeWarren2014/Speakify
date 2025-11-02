package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.constants.PackageNames
import com.mikewarren.speakify.data.db.UserAppModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppSettingsViewModel(
    val appModel: UserAppModel,
    val initialSettingsModel: AppSettingsModel,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    var isOpen by mutableStateOf(false)

    val modelFlow : StateFlow<AppSettingsModel?> = settingsRepository.appSettings
        .map { appSettings: Map<String, AppSettingsModel> ->
            Log.d(
                this.javaClass.name,
                "Mapping appSettings: $appSettings, for packageName: ${appModel.packageName}, result: ${appSettings[appModel.packageName]}"
            )

            return@map appSettings[appModel.packageName]
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = initialSettingsModel
        )

    private val _settings = MutableStateFlow(initialSettingsModel)
    val settings: StateFlow<AppSettingsModel> = _settings.asStateFlow()

    var childAnnouncerVoiceSectionViewModel: AnnouncerVoiceSectionViewModel? = null
    var childNotificationListViewModel: BaseNotificationSourceListViewModel<*>? = null

    fun getPackageName(): String {
        return appModel.packageName
    }

    init {
        viewModelScope.launch {
            modelFlow.collectLatest { model: AppSettingsModel? ->
                Log.d("AppSettingsViewModel", "modelFlow.collectLatest: $model")
                if (model == null) {
                    return@collectLatest
                }
                _settings.value = model


                childAnnouncerVoiceSectionViewModel = AnnouncerVoiceSectionViewModel(
                    settingsRepository = settingsRepository,
                    initialVoice = model.announcerVoice ?: Constants.DefaultTTSVoice,
                    onSave = { voiceName: String ->
                        _settings.update { model: AppSettingsModel ->
                            model.copy(announcerVoice = voiceName)
                        }
                    }
                )
                childNotificationListViewModel = createNotificationSourceListViewModel(model)



            }
        }
    }

    fun open() {
        isOpen = true

        childAnnouncerVoiceSectionViewModel?.onOpen()
        childNotificationListViewModel?.onOpen()
    }

    fun createNotificationSourceListViewModel(model: AppSettingsModel): BaseNotificationSourceListViewModel<*>? {
        Log.d("AppSettingsViewModel", "packageName = '${appModel.packageName}' , notificationSources = ${model.notificationSources}")

        // TODO: should we have a separate view model for each app?
        if ((getPackageName() in PackageNames.PhoneAppList) ||
            (getPackageName() in PackageNames.MessagingAppList) ||
            (getPackageName() == PackageNames.GoogleVoice)
        )
            return BaseImportantContactsListViewModel(
                settingsRepository,
                model.notificationSources,
                { importantContacts: List<String> ->
                    _settings.update { model: AppSettingsModel ->
                        model.copy(notificationSources = importantContacts)
                    }
                },
            )

        return null
}

    fun cancel() {
        childAnnouncerVoiceSectionViewModel?.cancel()
        childNotificationListViewModel?.cancel()

    }

    fun save() {
        childAnnouncerVoiceSectionViewModel?.onSave()
        childNotificationListViewModel?.onSave()
        viewModelScope.launch {
            settingsRepository.saveAppSettings(settings.value)
            Log.d("AppSettingsViewModel", "Saved settings: ${settings.value}")
        }
    }
}