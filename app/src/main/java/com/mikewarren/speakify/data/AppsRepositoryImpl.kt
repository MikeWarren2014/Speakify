package com.mikewarren.speakify.data

import android.content.Context
import com.mikewarren.speakify.data.db.UserAppsDao
import com.mikewarren.speakify.data.db.DbProvider
import com.mikewarren.speakify.data.db.UserAppModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class AppsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
): AppsRepository {
    private val userAppsDao : UserAppsDao = DbProvider.GetDb(context).userAppsDao()

    private val _importantApps = MutableStateFlow<List<UserAppModel>>(emptyList())
    override val importantApps: StateFlow<List<UserAppModel>> = _importantApps.asStateFlow()

    override suspend fun loadApps() {
        _importantApps.value = userAppsDao.getAll()
    }


    override suspend fun addImportantApp(appModel: UserAppModel) {
        userAppsDao.insert(appModel)

        _importantApps.update { it + appModel }
    }

    override suspend fun removeImportantApps(appsToRemove: List<UserAppModel>) {
        userAppsDao.deleteAll(*appsToRemove.toTypedArray())
        
        val setOfAppsToRemove: Set<UserAppModel> = appsToRemove.toSet()
        _importantApps.update { it - setOfAppsToRemove }

    }

}