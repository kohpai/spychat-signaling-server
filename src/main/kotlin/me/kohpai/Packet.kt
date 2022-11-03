package me.kohpai

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

enum class Command {
    CNT, SGN
}

@Serializable
data class Packet(
    val cmd: Command,
    val pubKey: String,
    @Serializable(with = ZonedDateTimeSerializer::class)
    val signedAt: ZonedDateTime,
    @Serializable(with = ByteArrayBase64Serializer::class)
    val signature: ByteArray,
    val data: String? = null,
)

class ByteArrayBase64Serializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "me.kohpai.ByteArrayBase64Serializer", PrimitiveKind.STRING
    )

    override fun deserialize(decoder: Decoder): ByteArray =
        Base64.getDecoder().decode(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: ByteArray) {
        encoder.encodeString(Base64.getEncoder().encodeToString(value))
    }

}

class ZonedDateTimeSerializer : KSerializer<ZonedDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "me.kohpai.ZonedDateTimeSerializer", PrimitiveKind.STRING
    )

    override fun deserialize(decoder: Decoder): ZonedDateTime =
        ZonedDateTime.parse(
            decoder.decodeString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME
        )

    override fun serialize(encoder: Encoder, value: ZonedDateTime) {
        encoder.encodeString(value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
    }
}