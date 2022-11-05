package me.kohpai

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.Test
import kotlin.test.assertEquals

class SerializationTest {
    @Test
    fun testSerializePacket() {
        val now = ZonedDateTime.now()
        val nowStr = now.format(DateTimeFormatter.ISO_INSTANT)
        val pubKey = "my_pub_key"
        val data = "my_data"
        val connectPacket = Packet.connect(pubKey, now)
        assertEquals(
            """{"cmd":"CNT","pubKey":"$pubKey","signedAt":"$nowStr"}""",
            Json.encodeToString(connectPacket)
        )

        val signalPacket = Packet.signal(pubKey, data, now)
        assertEquals(
            """{"cmd":"SGN","pubKey":"$pubKey","signedAt":"$nowStr","data":"$data"}""",
            Json.encodeToString(signalPacket)
        )
    }
}