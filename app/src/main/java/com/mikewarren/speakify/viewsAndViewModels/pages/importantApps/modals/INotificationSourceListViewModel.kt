package com.mikewarren.speakify.viewsAndViewModels.pages.importantApps.modals

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mikewarren.speakify.R
import com.mikewarren.speakify.data.NotificationSource
import com.mikewarren.speakify.viewsAndViewModels.widgets.IStringConverter
import com.mikewarren.speakify.viewsAndViewModels.widgets.UiText
import kotlinx.coroutines.flow.StateFlow

interface INotificationSourceListViewModel<T> : IAppSettingsSectionViewModel,
    IStringConverter<T> {
    var selectedNotificationSources: List<NotificationSource>

    val onSave: (List<NotificationSource>) -> Any

    val notificationSourcesFlow: StateFlow<List<NotificationSource>>

    fun isLoading(): Boolean

    fun getNotificationSourcesNameText() : UiText {
        return UiText.StringResource(R.string.notification_sources)
    }


    fun getMainDataStream(): StateFlow<List<T>>?
    fun getAddedSourceModels(): List<T>

    fun toNotificationSource(sourceModel: T): NotificationSource

    fun addNotificationSource(sourceModel: T) {
        addNotificationSource(toNotificationSource(sourceModel))
    }

    fun addNotificationSource(source: NotificationSource) {
        setNotificationSources(notificationSourcesFlow.value + source)
    }

    fun removeNotificationSource(sourceModel: T) {
        removeNotificationSource(toNotificationSource(sourceModel))
    }

    fun removeNotificationSource(source: NotificationSource) {
        setNotificationSources(notificationSourcesFlow.value - source)
    }

    override fun cancel() {
        setNotificationSources(selectedNotificationSources)
    }

    override fun onSave() {
        selectedNotificationSources = notificationSourcesFlow.value
        onSave(selectedNotificationSources)
    }

    fun setNotificationSources(notificationSources: List<NotificationSource>)
}