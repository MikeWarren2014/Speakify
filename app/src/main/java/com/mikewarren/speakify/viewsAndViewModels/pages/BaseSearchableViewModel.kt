package com.mikewarren.speakify.viewsAndViewModels.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.AppsRepository
import com.mikewarren.speakify.data.UserAppModel
import com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.AppListItemViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class BaseSearchableViewModel constructor(
    protected open var repository: AppsRepository
) : ViewModel() {
    protected abstract fun getMainMutableStateFlow(): MutableStateFlow<List<AppListItemViewModel>>
    protected abstract fun getRepositoryStateFlow(): StateFlow<List<UserAppModel>>

    private val _searchText = MutableStateFlow("")
    var searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _filteredApps = MutableStateFlow<List<AppListItemViewModel>>(emptyList())
    val filteredApps: StateFlow<List<AppListItemViewModel>> = _filteredApps.asStateFlow()

    protected fun onInit() {
        viewModelScope.launch {
            getRepositoryStateFlow().collect { userAppModels: List<UserAppModel> ->
                getMainMutableStateFlow().value = userAppModels.map { model: UserAppModel -> AppListItemViewModel(model) }
                _filteredApps.value = getMainMutableStateFlow().value
                applySearchFilter()
            }
        }
    }

    fun onSearchTextChange(text: String) {
        _searchText.value = text
        applySearchFilter()
    }

    fun applySearchFilter() {
        val text = _searchText.value
        val appViewModels = getMainMutableStateFlow().value
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
}