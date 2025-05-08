package com.mikewarren.speakify.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class AppsRepositoryImpl @Inject constructor(): AppsRepository {
    private var _allApps: List<UserAppModel> = emptyList()

    // TODO: In a real app, this would interact with a database or DataStore
    private val _importantApps = MutableStateFlow(_allApps
        .filter { model : UserAppModel -> model.enabled })
    override val importantApps: StateFlow<List<UserAppModel>> = _importantApps.asStateFlow()


    override fun addImportantApp(appModel: UserAppModel) {
        _importantApps.update { it + appModel }
    }

    override fun removeImportantApps(appsToRemove: List<UserAppModel>) {
        val setOfAppsToRemove: Set<UserAppModel> = appsToRemove.toSet()
        _importantApps.update { it - setOfAppsToRemove }
    }

    override fun updateApp(app: UserAppModel) {
        _importantApps.update {
            it.map { existingApp ->
                if (existingApp.packageName == app.packageName)
                    return@map app
                return@map existingApp
            }
        }
    }
}