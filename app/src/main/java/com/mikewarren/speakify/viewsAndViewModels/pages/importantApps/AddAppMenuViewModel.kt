package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.AppsRepository
import com.mikewarren.speakify.data.UserAppModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddAppMenuViewModel @Inject constructor(
    private val repository: AppsRepository // Replace with your actual repository
) : ViewModel() {
    private val _appsToAdd = MutableStateFlow<List<AppListItemViewModel>>(emptyList())

    private val _searchText = MutableStateFlow("")
    var searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _filteredApps = MutableStateFlow<List<AppListItemViewModel>>(_appsToAdd.value)
    val filteredApps: StateFlow<List<AppListItemViewModel>> = _filteredApps.asStateFlow()

    init {
        viewModelScope.launch {
            repository.otherApps.collect { userAppModels: List<UserAppModel> ->
                _appsToAdd.value = userAppModels.map { model: UserAppModel -> AppListItemViewModel(model) }
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
        val appViewModels = _appsToAdd.value
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

    fun onAppSelected(model: UserAppModel, onDone: (UserAppModel) -> Any) {
        _appsToAdd.update { appVMs : List<AppListItemViewModel> ->
            appVMs.filter({ vm: AppListItemViewModel -> vm.model.packageName != model.packageName })
        }

        applySearchFilter()

        onDone(model)
    }
}