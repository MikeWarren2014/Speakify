package com.mikewarren.speakify.data

import androidx.datastore.core.Serializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSettingsSerializer @Inject constructor() : Serializer<UserSettingsModel>{
    override val defaultValue: UserSettingsModel
        = UserSettingsModel(
        useDarkTheme = true,
        appSettings = emptyMap(),
        selectedTTSVoice = "",
    )

    override suspend fun readFrom(input: InputStream): UserSettingsModel {
        return Json.decodeFromString(UserSettingsModel.serializer(), input.readBytes().decodeToString())
    }

    override suspend fun writeTo(t: UserSettingsModel, output: OutputStream) {
        withContext(Dispatchers.IO) {
            output.write(
                Json
                    .encodeToString(UserSettingsModel.serializer(), t)
                    .encodeToByteArray()
            )
        }
    }
}