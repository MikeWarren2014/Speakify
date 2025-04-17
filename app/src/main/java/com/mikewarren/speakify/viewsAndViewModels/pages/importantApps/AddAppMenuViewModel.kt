package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.AppsRepository
import com.mikewarren.speakify.data.UserAppModel
import com.mikewarren.speakify.viewsAndViewModels.pages.BaseSearchableViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddAppMenuViewModel @Inject constructor(
    override var repository: AppsRepository // Replace with your actual repository
) : BaseSearchableViewModel(repository) {
    private val _appsToAdd = MutableStateFlow<List<AppListItemViewModel>>(emptyList())

    override fun getMainMutableStateFlow(): MutableStateFlow<List<AppListItemViewModel>> {
        return _appsToAdd
    }

    override fun getRepositoryStateFlow(): StateFlow<List<UserAppModel>> {
        return repository.otherApps
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