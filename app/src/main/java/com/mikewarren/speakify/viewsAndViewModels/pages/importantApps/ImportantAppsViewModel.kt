package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps

import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.AppsRepository
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.constants.PackageNames
import com.mikewarren.speakify.data.db.UserAppModel
import com.mikewarren.speakify.data.events.PackageListDataSource
import com.mikewarren.speakify.utils.AppNameHelper
import com.mikewarren.speakify.viewsAndViewModels.pages.BaseSearchableViewModel
import com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.AddAppMenuViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportantAppsViewModel @Inject constructor(
    override var repository: AppsRepository,
    private var settingsRepository: SettingsRepository,
    private val phonePermissionDataSource: PhonePermissionDataSource,
) : BaseSearchableViewModel(repository) {

    private val _importantApps = MutableStateFlow<List<AppListItemViewModel>>(emptyList())
    val importantApps: StateFlow<List<AppListItemViewModel>> = _importantApps.asStateFlow()

    val packageListDataSource = PackageListDataSource(settingsRepository.getContext())
    private val _allAppsFlow : StateFlow<List<ApplicationInfo>> = packageListDataSource.observeData()

    var childAddAppMenuViewModel: AddAppMenuViewModel? = null

    init {
        onInit()
    }

    public override fun onInit() {
        super.onInit()

        childAddAppMenuViewModel = AddAppMenuViewModel(repository,
            combine(_allAppsFlow, repository.importantApps) { allApps, importantApps ->
                val allAppsModels = allApps.map { appInfo ->
                    UserAppModel(
                        appName = AppNameHelper(settingsRepository.getContext())
                            .getAppDisplayName(appInfo),
                        packageName = appInfo.packageName,
                        enabled = false,
                    )
                }
                return@combine allAppsModels.filter {  model: UserAppModel ->
                    importantApps.find { it.packageName == model.packageName } == null
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
        )

    }

    fun handleNewAppPermissions() {
        viewModelScope.launch {
            repository.importantApps.collect { newAppsList ->
                checkForPhoneAppsAndRequestPermissions(newAppsList)

                val viewModels = newAppsList.map(onMapModelToVM())
                _importantApps.value = viewModels
            }
        }
    }


    private suspend fun checkForPhoneAppsAndRequestPermissions(apps: List<UserAppModel>) {
        // First, quick check: Do we even have any phone apps in the list?
        val hasPhoneApp = apps.any { PackageNames.PhoneAppList.contains(it.packageName) }

        if (!hasPhoneApp) return

        // Second, check if we have already requested permissions in the past.
        // We read this from the SettingsRepository to persist the state across app restarts/restores.
        val alreadyRequested = settingsRepository.hasRequestedPhonePermissions.first()

        if (!alreadyRequested) {
            Log.d("ImportantAppsVM", "Found phone app in list and haven't requested permissions yet. Requesting now.")

            // Trigger the request
            phonePermissionDataSource.requestPermissions()

            // Mark as requested so we never do this again
            settingsRepository.setPhonePermissionsRequested(true)
        }
    }

    fun fetchApps() {
        packageListDataSource.requestData()
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
        return importantApps.map { appListItemViewModels ->
            appListItemViewModels.map { it.model }
        }.stateIn(
            scope = viewModelScope, // Ties the flow to the ViewModel's lifecycle
            started = SharingStarted.WhileSubscribed(5000), // Stops when UI isn't listening
            initialValue = emptyList() // You MUST provide a default starting value
        )
    }


    fun getSelectedApps(): List<UserAppModel> {
        return _importantApps.value
            .filter { it.isSelected }
            .map { vm: AppListItemViewModel -> vm.model }
    }

    fun addApp(appModel: UserAppModel) {
        viewModelScope.launch {
            appModel.enabled = true
            repository.addImportantApp(appModel)
        }
    }

    fun deleteSelectedApps() {
        viewModelScope.launch {
            val selectedApps = getSelectedApps()
            selectedApps.forEach({ model: UserAppModel ->
                model.enabled = false
            })
            repository.removeImportantApps(selectedApps)
        }
    }

}