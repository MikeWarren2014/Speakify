package com.mikewarren.speakify.data.delegates

import android.content.Context
import androidx.datastore.core.DataStore
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.UserSettingsModel

interface ISettingsTest {
    var settingsRepository: SettingsRepository
    var userSettingsDataStore: DataStore<UserSettingsModel>
    fun setUpSettings(context: Context)
}