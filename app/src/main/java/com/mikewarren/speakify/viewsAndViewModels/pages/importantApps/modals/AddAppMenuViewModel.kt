package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.AppCategoryRepository
import com.mikewarren.speakify.data.AppsRepository
import com.mikewarren.speakify.data.OnboardingRepository
import com.mikewarren.speakify.data.db.UserAppModel
import com.mikewarren.speakify.data.models.AppCategory
import com.mikewarren.speakify.data.models.OnboardingCategorySelection
import com.mikewarren.speakify.viewsAndViewModels.pages.BaseSearchableViewModel
import com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.AppListItemViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

class AddAppMenuViewModel @Inject constructor(
    override var repository: AppsRepository,
    private val onboardingRepository: OnboardingRepository,
    private val appCategoryRepository: AppCategoryRepository,
    val allAppsFlow: StateFlow<List<UserAppModel>>
) : BaseSearchableViewModel(repository) {
    private val _appsToAdd = MutableStateFlow<List<AppListItemViewModel>>(emptyList())

    private var isFirstTime = true
    private var selectedCategories: List<OnboardingCategorySelection> = emptyList()
    private var categoryMap: Map<String, AppCategory> = emptyMap()

    override fun getMainMutableStateFlow(): MutableStateFlow<List<AppListItemViewModel>> {
        return _appsToAdd
    }

    override fun getRawDataStateFlow(): StateFlow<List<UserAppModel>> {
        return allAppsFlow
    }

    init {
        onInit()
        viewModelScope.launch {
            combine(onboardingRepository.importantAppCategories, allAppsFlow) { categories, apps ->
                categories to apps
            }.collect { (categories, apps) ->
                selectedCategories = categories
                
                // Perform bulk lookup for categories
                val packageNames = apps.map { it.packageName }
                categoryMap = appCategoryRepository.getCategoriesForPackages(packageNames)
                
                if (isFirstTime) {
                    applySearchFilter()
                }
            }
        }
    }

    override fun applySearchFilter() {
        val query = searchText.value
        
        val anyUnsatisfied = selectedCategories.any { !it.isSatisfied }

        if (!anyUnsatisfied || query.isNotEmpty()) {
             super.applySearchFilter()
             return
        }

        val allowedCategories = selectedCategories.map { it.category }.toSet()

        val appsInCategory = getMainMutableStateFlow().value
            .filter { vm: AppListItemViewModel -> 
                val category = categoryMap[vm.model.packageName]
                category != null && allowedCategories.contains(category)
            }

        _filteredApps.value = appsInCategory
    }

    fun onAppSelected(model: UserAppModel, onDone: (UserAppModel) -> Any) {
        isFirstTime = false
        _appsToAdd.update { appVMs : List<AppListItemViewModel> ->
            appVMs.filter { vm: AppListItemViewModel -> vm.model.packageName != model.packageName }
        }

        applySearchFilter()

        onDone(model)
    }
}
