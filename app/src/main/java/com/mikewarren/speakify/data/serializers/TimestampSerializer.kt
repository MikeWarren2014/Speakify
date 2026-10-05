package com.mikewarren.speakify.data.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimestampSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Timestamp", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Long) {
        val rfc3339String = millisToTimestampString(value)
        encoder.encodeString(rfc3339String)
    }

    fun millisToTimestampString(epochMillis: Long): String {
        val dateTime = toLocalDateTime(epochMillis)
        return dateFormatter.format(dateTime)
    }

    fun toLocalDateTime(epochMillis: Long): LocalDateTime {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
    }

    override fun deserialize(decoder: Decoder): Long {
        val rfc3339String = decoder.decodeString()
        return millisFromTimestampString(rfc3339String)
    }

    fun millisFromTimestampString(rfc3339String: String): Long {
        val dateTime = LocalDateTime.parse(rfc3339String, dateFormatter)
        return dateTime.atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'")
        .withZone(ZoneId.systemDefault())
}
