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

    override val importantApps = userAppsDao.getAllFlow()


    override suspend fun addImportantApp(appModel: UserAppModel) {
        userAppsDao.insert(appModel)
    }

    override suspend fun removeImportantApps(appsToRemove: List<UserAppModel>) {
        userAppsDao.deleteAll(*appsToRemove.toTypedArray())

    }

}