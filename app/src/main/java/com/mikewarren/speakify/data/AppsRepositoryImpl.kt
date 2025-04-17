package com.mikewarren.speakify.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class AppsRepositoryImpl @Inject constructor(): AppsRepository {
    private val _allApps = listOf(
        UserAppModel("com.example.app1", "App One", true),
        UserAppModel("com.example.app2", "App Two", true),
        UserAppModel("com.example.app3", "App Three", true),
        UserAppModel("com.example.app4", "Example App 1", false),
        UserAppModel("com.example.app5", "Another App", false),
        UserAppModel("com.test.game", "Awesome Game", false),
    )

    // TODO: In a real app, this would interact with a database or DataStore
    private val _importantApps = MutableStateFlow(_allApps
        .filter { model : UserAppModel -> model.enabled })
    override val importantApps: StateFlow<List<UserAppModel>> = _importantApps.asStateFlow()

    private val _otherApps = MutableStateFlow(_allApps.filter
    { model: UserAppModel -> !model.enabled })
    override val otherApps: StateFlow<List<UserAppModel>> = _otherApps.asStateFlow()

    override fun addImportantApp(appModel: UserAppModel) {
        _importantApps.update { it + appModel }
        _otherApps.update { it - setOf(appModel) }
    }

    override fun removeImportantApps(appsToRemove: List<UserAppModel>) {
        val setOfAppsToRemove: Set<UserAppModel> = appsToRemove.toSet()
        _importantApps.update { it - setOfAppsToRemove }
        _otherApps.update { it + setOfAppsToRemove }
    }

    override fun updateApp(app: UserAppModel) {
        _importantApps.update {
            it.map { existingApp ->
                if (existingApp.packageName == app.packageName) app else existingApp
            }
        }
    }
}