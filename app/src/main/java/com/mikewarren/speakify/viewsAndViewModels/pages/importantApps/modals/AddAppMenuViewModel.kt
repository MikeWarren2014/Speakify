package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import com.mikewarren.speakify.data.AppsRepository
import com.mikewarren.speakify.data.UserAppModel
import com.mikewarren.speakify.viewsAndViewModels.pages.BaseSearchableViewModel
import com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.AppListItemViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class AddAppMenuViewModel @Inject constructor(
    override var repository: AppsRepository,
    val allAppsFlow: StateFlow<List<UserAppModel>>
) : BaseSearchableViewModel(repository) {
    private val _appsToAdd = MutableStateFlow<List<AppListItemViewModel>>(emptyList())

    override fun getMainMutableStateFlow(): MutableStateFlow<List<AppListItemViewModel>> {
        return _appsToAdd
    }

    override fun getRawDataStateFlow(): StateFlow<List<UserAppModel>> {
        return allAppsFlow
    }

    init {
        onInit()
    }

    fun onAppSelected(model: UserAppModel, onDone: (UserAppModel) -> Any) {
        _appsToAdd.update { appVMs : List<AppListItemViewModel> ->
            appVMs.filter({ vm: AppListItemViewModel -> vm.model.packageName != model.packageName })
        }

        applySearchFilter()

        onDone(model)
    }
}