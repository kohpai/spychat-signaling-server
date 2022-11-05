package me.kohpai

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

enum class Command {
    CNT, SGN
}

@Serializable
data class Packet(
    val cmd: Command,
    val pubKey: String,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val signedAt: ZonedDateTime,
    val data: String? = null,
) {
    companion object {
        fun connect(pubKey: String, signedAt: ZonedDateTime) =
            Packet(Command.CNT, pubKey, signedAt)

        fun signal(pubKey: String, data: String, signedAt: ZonedDateTime) =
            Packet(Command.SGN, pubKey, signedAt, data)
    }
}

class ZonedDateTimeSerializer : KSerializer<ZonedDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "me.kohpai.ZonedDateTimeSerializer", PrimitiveKind.STRING
    )

    override fun deserialize(decoder: Decoder): ZonedDateTime =
        ZonedDateTime.parse(
            decoder.decodeString(),
            DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault())
        )

    override fun serialize(encoder: Encoder, value: ZonedDateTime) {
        encoder.encodeString(value.format(DateTimeFormatter.ISO_INSTANT))
    }
}