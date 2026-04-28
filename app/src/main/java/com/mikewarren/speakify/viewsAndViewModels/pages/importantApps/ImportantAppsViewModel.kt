package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps

import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.AppsRepository
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.constants.PackageNames
import com.mikewarren.speakify.data.db.UserAppModel
import com.mikewarren.speakify.data.events.PackageListDataRequester
import com.mikewarren.speakify.services.TTSManager
import com.mikewarren.speakify.utils.AppNameHelper
import com.mikewarren.speakify.viewsAndViewModels.pages.BaseSearchableViewModel
import com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals.AddAppMenuViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubstituteAppCandidate(
    val missingApp: UserAppModel,
    val substitutes: List<UserAppModel>
)

@HiltViewModel
class ImportantAppsViewModel @Inject constructor(
    override var repository: AppsRepository,
    private var settingsRepository: SettingsRepository,
    private val ttsManager: TTSManager,
    private val phonePermissionRequester: PhonePermissionRequester,
) : BaseSearchableViewModel(repository) {

    private val _importantApps = MutableStateFlow<List<AppListItemViewModel>>(emptyList())
    val importantApps: StateFlow<List<AppListItemViewModel>> = _importantApps.asStateFlow()

    private val _selectedCount = MutableStateFlow(0)
    val selectedCount: StateFlow<Int> = _selectedCount.asStateFlow()

    val packageListDataSource = PackageListDataRequester.GetInstance(settingsRepository.getContext())
    private val _allAppsFlow : StateFlow<List<ApplicationInfo>> = packageListDataSource.observeData()
    val isLoading: StateFlow<Boolean> = packageListDataSource.isLoading

    var childAddAppMenuViewModel: AddAppMenuViewModel? = null

    private val _ignoredSubstitutePackages = MutableStateFlow<Set<String>>(emptySet())

    val substituteCandidates: StateFlow<List<SubstituteAppCandidate>> = combine(
        repository.importantApps,
        _allAppsFlow,
        _ignoredSubstitutePackages
    ) { importantApps, allInstalledApps, ignoredPackages ->
        val installedPackageNames = allInstalledApps.map { it.packageName }.toSet()
        val importantPackageNames = importantApps.map { it.packageName }.toSet()

        val candidates = mutableListOf<SubstituteAppCandidate>()

        importantApps.filter { it.packageName !in installedPackageNames && it.packageName !in ignoredPackages }
            .forEach { missingApp ->
                val substitutePackageNames = when {
                    PackageNames.PhoneAppList.contains(missingApp.packageName) -> PackageNames.PhoneAppList
                    PackageNames.MessagingAppList.contains(missingApp.packageName) -> PackageNames.MessagingAppList
                    else -> null
                }

                if (substitutePackageNames != null) {
                    val availableSubstitutes = allInstalledApps
                        .filter { it.packageName in substitutePackageNames && it.packageName !in importantPackageNames }
                        .map { appInfo ->
                            UserAppModel(
                                appName = AppNameHelper(settingsRepository.getContext())
                                    .getAppDisplayName(appInfo),
                                packageName = appInfo.packageName,
                                enabled = true
                            )
                        }

                    if (availableSubstitutes.isNotEmpty()) {
                        candidates.add(SubstituteAppCandidate(missingApp, availableSubstitutes))
                    }
                }
            }
        candidates
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        onInit()
    }

    override fun onInit() {
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
            }
        }
    }


    private fun checkForPhoneAppsAndRequestPermissions(apps: List<UserAppModel>) {
        // First, quick check: Do we even have any phone apps in the list?
        val hasPhoneApp = apps.any { PackageNames.PhoneAppList.contains(it.packageName) }

        if (!hasPhoneApp) return

        Log.d("ImportantAppsVM", "Found phone app in list and haven't requested permissions yet. Requesting now.")

        // Trigger the request. It does NOTHING if we already have the phone-related permissions
        phonePermissionRequester.requestPermissions()

    }

    fun fetchApps() {
        packageListDataSource.requestData()
    }

    override fun onMapModelToVM(): (UserAppModel) -> AppListItemViewModel {
        return { model: UserAppModel -> ConfigurableAppListItemViewModel(model,
                settingsRepository,
                ttsManager,
                {
                    updateSelectedCount()
                },
            )
        }
    }

    private fun updateSelectedCount() {
        _selectedCount.value = getSelectedApps().count()
    }

    override fun getMainMutableStateFlow(): MutableStateFlow<List<AppListItemViewModel>> {
        return _importantApps
    }

    override fun getRawDataStateFlow(): StateFlow<List<UserAppModel>> {
        // Return the repository flow directly, breaking the circular dependency
        return repository.importantApps.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
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
            // Reset selection count after deletion
            _selectedCount.value = 0
        }
    }

    fun substituteApp(missingApp: UserAppModel, substitute: UserAppModel) {
        viewModelScope.launch {
            repository.substituteImportantApp(missingApp, substitute)
        }
    }

    fun ignoreSubstituteCandidate(candidate: SubstituteAppCandidate) {
        _ignoredSubstitutePackages.update { it + candidate.missingApp.packageName }
    }

}
