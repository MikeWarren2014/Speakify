package com.mikewarren.speakify.data.delegates

import android.content.Context
import androidx.datastore.core.DataStore
import com.mikewarren.speakify.data.SettingsRepository
import com.mikewarren.speakify.data.SettingsRepositoryImpl
import com.mikewarren.speakify.data.UserSettingsModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking

class SettingsTestDelegate: ISettingsTest {
    override lateinit var settingsRepository: SettingsRepository
    override lateinit var userSettingsDataStore: DataStore<UserSettingsModel>

    override fun setUpSettings(context: Context) {
        userSettingsDataStore = mockk(relaxed = true)
        val userSettingsFlow = MutableStateFlow(UserSettingsModel())
        every { userSettingsDataStore.data } returns userSettingsFlow
        coEvery { userSettingsDataStore.updateData(any()) } answers {
            val transform = firstArg<suspend (UserSettingsModel) -> UserSettingsModel>()
            val newValue = runBlocking { transform(userSettingsFlow.value) }
            userSettingsFlow.value = newValue
            newValue
        }

        settingsRepository = SettingsRepositoryImpl(context, userSettingsDataStore)
    }
}