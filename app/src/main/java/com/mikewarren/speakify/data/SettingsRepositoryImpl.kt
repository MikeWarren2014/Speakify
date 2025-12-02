// SettingsRepositoryImpl.kt
package com.mikewarren.speakify.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.mikewarren.speakify.data.db.AppSettingsDao
import com.mikewarren.speakify.data.db.AppSettingsDbModel
import com.mikewarren.speakify.data.db.AppSettingsWithNotificationSources
import com.mikewarren.speakify.data.db.DbProvider
import com.mikewarren.speakify.data.db.NotificationSourceModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val Context.userSettingsDataStore: DataStore<UserSettingsModel> by dataStore(
    fileName = "userSettings.pb",
    serializer = UserSettingsSerializer(),
)


class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {
    private val _db = DbProvider.GetDb(context)

    override val appSettings: Flow<Map<String, AppSettingsModel>> = _db.appSettingsDao().getAllFlow()
        .map { list ->
            // Map the list from the DB into your Map<PackageName, Model> structure
            list.mapNotNull { nestedModel ->
                val model = AppSettingsModel.FromDbModel(nestedModel)
                if (model != null) {
                    return@mapNotNull model.packageName to model
                }
                return@mapNotNull null
            }.toMap()
        }
        // We use stateIn to keep the latest value cached (hot flow), similar to your previous behavior
        .stateIn(
            scope = CoroutineScope(Dispatchers.IO), // Keep it alive as long as the Repo is alive (Singleton)
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    override val useDarkTheme: Flow<Boolean?> = context.userSettingsDataStore.data
        .map { model: UserSettingsModel ->
            model.useDarkTheme
        }

    override val selectedTTSVoice: Flow<String?> = context.userSettingsDataStore.data
        .map { model: UserSettingsModel ->
            model.selectedTTSVoice
        }

    override val maximizeVolumeOnScreenOff: Flow<Boolean> = context.userSettingsDataStore.data
        .map { model: UserSettingsModel ->
            model.maximizeVolumeOnScreenOff
        }

    override val minVolume: Flow<Int> = context.userSettingsDataStore
        .data
        .map { model: UserSettingsModel ->
            model.minVolume
        }


    override suspend fun updateUseDarkTheme(useDarkTheme: Boolean) {
        context.userSettingsDataStore.updateData { model: UserSettingsModel ->
            model.copy(useDarkTheme = useDarkTheme)
        }
    }

    override suspend fun saveSelectedVoice(voiceName: String) {
        context.userSettingsDataStore.updateData { model: UserSettingsModel ->
            model.copy(selectedTTSVoice = voiceName)
        }
    }

    override suspend fun setMaximizeVolumeOnScreenOff(shouldMaximize: Boolean) {
        context.userSettingsDataStore.updateData { model: UserSettingsModel ->
            model.copy(maximizeVolumeOnScreenOff = shouldMaximize)
        }
    }

    override suspend fun setMinVolume(volume: Int) {
        context.userSettingsDataStore.updateData { model: UserSettingsModel ->
            model.copy(minVolume = volume)
        }
    }

    override suspend fun saveAppSettings(appSettingsModel: AppSettingsModel) {
        val appSettingsDao = _db.appSettingsDao()
        val notificationSourcesDao = _db.notificationSourcesDao()

        // create the AppSettingsWithNotificationSources object from our AppSettingsModel
        val appSettingsDbModel = AppSettingsDbModel(
            id = appSettingsModel.id,
            packageName = appSettingsModel.packageName,
            announcerVoice = appSettingsModel.announcerVoice,
        )

        if (appSettingsModel.id != null) {
            notificationSourcesDao.deleteAllWithoutValues(appSettingsModel.id, appSettingsModel.notificationSources)
        }

        val savedAppSettingsId = saveToDatabase(appSettingsDao, appSettingsDbModel)

        notificationSourcesDao.insertAll(appSettingsModel.notificationSources.map { value: String ->
            NotificationSourceModel(
                id = null,
                appSettingsId = savedAppSettingsId,
                value,
            )
        })

    }

    private suspend fun saveToDatabase(appSettingsDao: AppSettingsDao, appSettingsDbModel: AppSettingsDbModel) : Long {
        if (appSettingsDbModel.id == null) {
            return appSettingsDao.insert(appSettingsDbModel)

        }
        appSettingsDao.updateAppSettings(appSettingsDbModel)
        return appSettingsDbModel.id
    }

    override fun getContext() : Context {
        return context
    }

    // TODO: this shouldn't be here in SettingsRepository, but right now I can't think of a better place to put it

    override val hasRequestedPhonePermissions: Flow<Boolean> = context.userSettingsDataStore.data
        .map { model: UserSettingsModel ->
            model.hasRequestedPhonePermissions
        }

    override suspend fun setPhonePermissionsRequested(hasRequested: Boolean) {
        context.userSettingsDataStore.updateData { model: UserSettingsModel ->
            model.copy(hasRequestedPhonePermissions = hasRequested)
        }
    }
}