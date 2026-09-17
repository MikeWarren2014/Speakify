package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.Constants
import com.mikewarren.speakify.data.NotificationSource
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.db.UserAppModel
import com.mikewarren.speakify.services.TTSManager
import com.mikewarren.speakify.strategies.GeneratedAdditionalSettingsRegistry
import com.mikewarren.speakify.strategies.GeneratedNotificationListRegistry
import com.mikewarren.speakify.strategies.NotificationStrategyRegistry
import com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.widgets.BaseAppAdditionalSettingsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.reflect.full.primaryConstructor

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
    var childNotificationListViewModel: INotificationSourceListViewModel<*>? = null
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

    fun createNotificationSourceListViewModel(model: AppSettingsModel): INotificationSourceListViewModel<*>? {
        Log.d("AppSettingsViewModel", "packageName = '${appModel.packageName}' , notificationSources = ${model.notificationSources}")

        val packageName = getPackageName()
        val kClass = NotificationStrategyRegistry.findComponentClass(
            packageName,
            GeneratedNotificationListRegistry.classMap
        )

        if (kClass == null) {
            return null
        }

        return kClass.primaryConstructor?.call(
            settingsRepository,
            model.notificationSources,
            { importantContacts: List<NotificationSource> ->
                _settings.update { model: AppSettingsModel ->
                    model.copy(notificationSources = importantContacts)
                }
            }
        ) as? INotificationSourceListViewModel<*>

    }

    private fun createAdditionalSettingsViewModel(model: AppSettingsModel): BaseAppAdditionalSettingsViewModel? {
        val packageName = getPackageName()
        val kClass = NotificationStrategyRegistry.findComponentClass(
            packageName,
            GeneratedAdditionalSettingsRegistry.classMap
        )

        if (kClass == null) {
            return null
        }

        return kClass.primaryConstructor?.call(
            settingsRepository,
            model.additionalSettings,
            { additionalSettings: Map<String, String> ->
                _settings.update { model: AppSettingsModel ->
                    model.copy(additionalSettings = additionalSettings)
                }
            }
        ) as? BaseAppAdditionalSettingsViewModel
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