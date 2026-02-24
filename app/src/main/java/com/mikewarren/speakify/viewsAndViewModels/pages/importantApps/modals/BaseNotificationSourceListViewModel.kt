package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.viewsAndViewModels.widgets.BaseModelAutoCompletableViewModel
import com.mikewarren.speakify.viewsAndViewModels.widgets.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseNotificationSourceListViewModel<T>(
    override var settingsRepository: SettingsRepository,
    protected var notificationSourceList: List<String>,
    val onSave: (List<String>) -> Any,
) : IAppSettingsSectionViewModel,
    BaseModelAutoCompletableViewModel<T>() {
    protected var selectedNotificationSources = notificationSourceList
    abstract val allData: StateFlow<List<T>>

    protected val _notificationSources = MutableStateFlow(notificationSourceList)
    val notificationSources: StateFlow<List<String>> = _notificationSources.asStateFlow()

    protected val _allAddableSourceModels = MutableStateFlow<List<T>>(emptyList())
    val allAddableSourceModels: StateFlow<List<T>> = _allAddableSourceModels.asStateFlow()

    var isLoading by mutableStateOf(false)
        protected set

    override fun onOpen() {
        Log.d(this.javaClass.name,
            "Invoked BaseNotificationSourceList.onOpen()")
        isLoading = true
        viewModelScope.launch {
            allData.collectLatest {
                Log.d(this.javaClass.name,
                    "Data loaded successfully: ${it}")
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
        Log.d(this.javaClass.name,
            "Invoked BaseNotificationSourceList.getAddedSourceModels() ... allData.value == ${allData.value}")
        return allData.value?.filter { model: T -> toSourceString(model) in _notificationSources.value }
            ?: emptyList()

    }

    override fun getLabelText(): UiText {
        return UiText.StringResource(R.string.autocomplete_label_notifications)
    }

    open fun getNotificationSourcesNameText() : UiText {
        return UiText.StringResource(R.string.notification_sources)
    }

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
        onSave(selectedNotificationSources)
    }
}