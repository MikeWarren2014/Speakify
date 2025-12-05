package com.mikewarren.speakify.data

import com.mikewarren.speakify.data.db.UserAppModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AppsRepository {

    val importantApps: Flow<List<UserAppModel>>

    suspend fun addImportantApp(appModel: UserAppModel)
    suspend fun removeImportantApps(appsToRemove: List<UserAppModel>)
}