package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps

import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.mikewarren.speakify.data.AppsRepository
import com.mikewarren.speakify.data.AppsRepositoryImpl
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.UserAppModel
import com.mikewarren.speakify.viewsAndViewModels.pages.BaseSearchableViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class ImportantAppsViewModel @Inject constructor(
    override var repository: AppsRepository,
    private var settingsRepository: SettingsRepository,
) : BaseSearchableViewModel(repository) {

    private val _importantApps = MutableStateFlow<List<AppListItemViewModel>>(emptyList())
    val importantApps: StateFlow<List<AppListItemViewModel>> = _importantApps.asStateFlow()

    init {
        onInit()
    }

    override fun onMapModelToVM(): (UserAppModel) -> AppListItemViewModel {
        return { model: UserAppModel -> ConfigurableAppListItemViewModel(model, settingsRepository) }
    }

    override fun getMainMutableStateFlow(): MutableStateFlow<List<AppListItemViewModel>> {
        return _importantApps
    }

    override fun getRepositoryStateFlow(): StateFlow<List<UserAppModel>> {// Temporary: Observe readiness (with a timeout for safety)
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