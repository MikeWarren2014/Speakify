package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps

import android.content.pm.ApplicationInfo
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.AppsRepository
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.UserAppModel
import com.mikewarren.speakify.data.events.PackagesListDataSource
import com.mikewarren.speakify.viewsAndViewModels.pages.BaseSearchableViewModel
import com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.AddAppMenuViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ImportantAppsViewModel @Inject constructor(
    override var repository: AppsRepository,
    private var settingsRepository: SettingsRepository,
) : BaseSearchableViewModel(repository) {

    private val _importantApps = MutableStateFlow<List<AppListItemViewModel>>(emptyList())
    val importantApps: StateFlow<List<AppListItemViewModel>> = _importantApps.asStateFlow()

    val packagesListDataSource = PackagesListDataSource(settingsRepository.getContext())
    private val _allAppsFlow : StateFlow<List<ApplicationInfo>> = packagesListDataSource.observeData()

    var childAddAppMenuViewModel: AddAppMenuViewModel? = null

    init {
        onInit()
    }

    override fun onInit() {
        super.onInit()

        childAddAppMenuViewModel = AddAppMenuViewModel(repository,
            combine(_allAppsFlow, repository.importantApps) { allApps, importantApps ->
                val allAppsModels = allApps.map { appInfo ->
                    UserAppModel(
                        appName = settingsRepository.getContext().packageManager
                            .getApplicationLabel(appInfo)
                            .toString(),
                        packageName = appInfo.packageName,
                        enabled = false,
                    )
                }
                val importantAppsModels = importantApps
                allAppsModels.minus(importantAppsModels.toSet())
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
        )
    }

    fun fetchApps() {
        packagesListDataSource.requestData()
    }

    override fun onMapModelToVM(): (UserAppModel) -> AppListItemViewModel {
        return { model: UserAppModel -> ConfigurableAppListItemViewModel(model,
            settingsRepository,
        )
        }
    }

    override fun getMainMutableStateFlow(): MutableStateFlow<List<AppListItemViewModel>> {
        return _importantApps
    }

    override fun getRawDataStateFlow(): StateFlow<List<UserAppModel>> {
        return repository.importantApps
    }


    fun getSelectedApps(): List<UserAppModel> {
        return _importantApps.value
            .filter { it.isSelected }
            .map { vm: AppListItemViewModel -> vm.model }
    }

    fun addApp(appModel: UserAppModel) {
        appModel.enabled = true
        repository.addImportantApp(appModel)
    }

    fun deleteSelectedApps() {
        val selectedApps = getSelectedApps()
        selectedApps.forEach({ model: UserAppModel ->
            model.enabled = false
        })
        repository.removeImportantApps(selectedApps)
    }

    fun updateAppConfig(app: UserAppModel) {
        repository.updateApp(app)
    }

}