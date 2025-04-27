package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.data.AppSettingsModel
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.viewsAndViewModels.widgets.BaseAutoCompletableViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseNotificationSourceListViewModel<T>(
    override var settingsRepository: SettingsRepository,
    protected var selectedNotificationSources: List<String>,
    val onSave: (List<String>) -> Any,
) : IAppSettingsSectionViewModel,
    BaseAutoCompletableViewModel() {

    abstract val allData: StateFlow<List<T>>

    protected val _notificationSources = MutableStateFlow<List<String>>(emptyList())
    val notificationSources: StateFlow<List<String>> = _notificationSources.asStateFlow()

    protected val _allAddableSourceModels = MutableStateFlow<List<T>>(emptyList())
    val allAddableSourceModels: StateFlow<List<T>> = _allAddableSourceModels.asStateFlow()

    var isLoading by mutableStateOf(false)
        protected set

    fun onOpen() {
        isLoading = true
        viewModelScope.launch {
            allData.collectLatest {
                onDataLoaded()
            }
        }
    }

    fun onDataLoaded() {
        searchText = ""
        _allAddableSourceModels.update { allData.value?.minus(getAddedSourceModels().toSet()) ?: emptyList() }
        isLoading = false
    }
    
    fun getAddedSourceModels(): List<T> {
        return allData.value?.filter { model: T -> toSourceString(model) in _notificationSources.value }
            ?: emptyList()

    }

    override fun getLabel(): String {
        return "Notification Sources"
    }

    open fun getNotificationSourcesName() : String {
        return "notification sources"
    }

    abstract fun toViewString(sourceModel: T): String
    abstract fun toSourceString(sourceModel: T): String

    fun addNotificationSource(selection: String) {
        val sourceModel = allAddableSourceModels.value.find { toViewString(it) == selection }
        if (sourceModel == null)
            throw IllegalStateException("Notification source $selection not found")

        viewModelScope.launch {
            val updatedSources = _notificationSources.value + toSourceString(sourceModel)
            _notificationSources.value = updatedSources

            onRemoveAddableSource(sourceModel)
        }
    }

    fun removeNotificationSource(sourceModel: T) {
        val source = toSourceString(sourceModel)

        viewModelScope.launch {
            val updatedSources = _notificationSources.value - source
            _notificationSources.value = updatedSources

            onAddAddableSource(sourceModel)
        }
    }

    protected fun onRemoveAddableSource(sourceModel: T) {
        viewModelScope.launch {
            val updatedSources = _allAddableSourceModels.value - sourceModel
            _allAddableSourceModels.value = updatedSources
        }
    }
    protected fun onAddAddableSource(sourceModel: T) {
        viewModelScope.launch {
            val updatedSources = _allAddableSourceModels.value + sourceModel
            _allAddableSourceModels.value = updatedSources
        }
    }

    override fun cancel() {
        searchText = ""
        _notificationSources.value = selectedNotificationSources
    }

    override fun onSave() {
        selectedNotificationSources = notificationSources.value
        onSave(notificationSources.value)
    }
}