package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.AppsRepository
import com.mikewarren.speakify.data.UserAppModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportantAppsViewModel @Inject constructor(
    private val repository: AppsRepository // Replace with your actual repository
) : ViewModel() {

    private val _importantApps = MutableStateFlow<List<AppListItemViewModel>>(emptyList())
    val importantApps: StateFlow<List<AppListItemViewModel>> = _importantApps.asStateFlow()

    private val _searchText = MutableStateFlow("")
    var searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _filteredApps = MutableStateFlow<List<AppListItemViewModel>>(emptyList())
    val filteredApps: StateFlow<List<AppListItemViewModel>> = _filteredApps.asStateFlow()

    init {
        viewModelScope.launch {
            repository.importantApps.collect { userAppModels: List<UserAppModel> ->
                _importantApps.value = userAppModels.map { model: UserAppModel -> AppListItemViewModel(model) }

                // Update _filteredApps after the main state flow has been populated
                _filteredApps.value = _importantApps.value

                applySearchFilter()
            }
        }
    }

    fun onSearchTextChange(text: String) {
        _searchText.value = text
        applySearchFilter()
    }

    private fun applySearchFilter() {
        val text = _searchText.value
        val appViewModels = _importantApps.value
        if (text.isBlank()) {
            _filteredApps.value = appViewModels
            return;
        }
        _filteredApps.value = appViewModels.filter { vm: AppListItemViewModel ->
            vm.model.appName.contains(
                text,
                ignoreCase = true
            )
        }
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