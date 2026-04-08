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
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val defaultValue: UserSettingsModel
        = UserSettingsModel()

    override suspend fun readFrom(input: InputStream): UserSettingsModel {
        return try {
            json.decodeFromString(
                UserSettingsModel.serializer(),
                input.readBytes().decodeToString()
            )
        } catch (e: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: UserSettingsModel, output: OutputStream) {
        withContext(Dispatchers.IO) {
            output.write(
                json.encodeToString(UserSettingsModel.serializer(), t)
                    .encodeToByteArray()
            )
        }
    }
}
