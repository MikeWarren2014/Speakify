package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.NotificationSource
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.viewsAndViewModels.widgets.BaseAutoCompletableViewModel
import com.mikewarren.speakify.viewsAndViewModels.widgets.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseAutoCompletableNotificationSourceListViewModel<T>(
    override var settingsRepository: SettingsRepository,
    override var selectedNotificationSources: List<NotificationSource>,
    override val onSave: (List<NotificationSource>) -> Any,
) :
    INotificationSourceListViewModel<T>,
    BaseAutoCompletableViewModel<T>(){

    protected val _notificationSourcesFlow = MutableStateFlow(selectedNotificationSources)
    override val notificationSourcesFlow: StateFlow<List<NotificationSource>> = _notificationSourcesFlow.asStateFlow()

    abstract val allData: StateFlow<List<T>>

    protected val _allAddableSourceModels = MutableStateFlow<List<T>>(emptyList())
    val allAddableSourceModels: StateFlow<List<T>> = _allAddableSourceModels.asStateFlow()

    var isDataLoading by mutableStateOf(false)
        protected set

    override fun isLoading(): Boolean { return isDataLoading }

    override fun onOpen() {
        isDataLoading = true
        viewModelScope.launch {
            allData.collectLatest {
                onDataLoaded()
            }
        }
    }

    fun onDataLoaded() {
        searchText = ""
        _allAddableSourceModels.update { allData.value.minus(getAddedSourceModels().toSet()) ?: emptyList() }
        isDataLoading = false
        isDisabled = allData.value.isEmpty()
    }

    override fun setNotificationSources(notificationSources: List<NotificationSource>) {
        _notificationSourcesFlow.value = notificationSources
    }


    override fun getLabelText(): UiText {
        return UiText.StringResource(R.string.autocomplete_label_notifications)
    }

    override fun getMainDataStream(): StateFlow<List<T>>? {
        return allData
    }

    override fun getAddedSourceModels(): List<T> {
        return allData.value
            .filter { model: T -> toSourceString(model) in _notificationSourcesFlow.value.map { it.value } }
    }

    fun addNotificationSource(selection: String) {
        val sourceModel = allData.value.find { toViewString(it) == selection } ?: return

        searchText = ""

        viewModelScope.launch {
            addNotificationSource(toNotificationSource(sourceModel))

            onRemoveAddableSource(sourceModel)
        }
    }

    // TODO: need to come up with more intuitive name for this method
    fun onRemoveAddableSource(sourceModel: T) {
        val updatedSources = _allAddableSourceModels.value - sourceModel
        _allAddableSourceModels.value = updatedSources
    }

    override fun removeNotificationSource(sourceModel: T) {
        super.removeNotificationSource(sourceModel)
        onAddAddableSource(sourceModel)
    }

    // TODO: need to come up with more intuitive names for this method
    fun onAddAddableSource(sourceModel: T) {
        val updatedSources = _allAddableSourceModels.value + sourceModel
        _allAddableSourceModels.value = updatedSources
    }

    override fun cancel() {
        super.cancel()
        searchText = ""
    }
}