package me.kohpai

import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.kohpai.crypto.ECDSAContent
import me.kohpai.crypto.ECPEMReader
import java.io.File
import java.security.PrivateKey
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.Date
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    private var alicePrivateKey: PrivateKey
    private var bobPrivateKey: PrivateKey

    private val alicePublicKey =
        "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEWYsCG1SsWZlYJT8yV1TVJIgkpxTYliWKAgL5Eotx2cX6bAVlX+G4folAl5q6fo/fcq1B4QKaWkHBODXz5J+yPa1s1gnIwCqxpdo0nqAd9JmEJPxO0oaNTk8nZSnObQVe"
    private val aliceSignature =
        "MGUCMAx/4E4+cTHl8C1/MpaY5UwWhJpive1SS+nEGM34wvfAwnM2wzjIc4Jx/kCasHkc2QIxAJWvTx0jHhyYsB7yxdvSsg+D9DtyLTC4lgsNl5w1bFXhxDigt2Jzqd5M7oX5/3SE5w=="
    private val bobPublicKey =
        "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEECK5wO0XZQR654QS00UFKxTVNcD72ESaq9JTOtEB8XI3imxIiCHQih7aymBGZESKYKamy8bR9vwiBK87o0IEykFrNkQE5T1lchipihb6tfrhet3CH5C/7z3nJmiFtSu/"
    private val bobSignature =
        "MGQCMHAcjskdykriWxwstMhSAmXsVo6pBFGDmLyPxz54D1GThDdDOLECsa4lGZAnel49tAIwTGoQjk3iw3H/bA7EQ2sUncKOJqPxPVwtercmEEYX1DM0LXCmmh1pZtTUdyaRL3CQ"

    private val base64Encoder = Base64.getEncoder()

    init {
        val aliceKeyFile =
            File("src/test/resources/alice_ec_private.pem").bufferedReader()
        alicePrivateKey = ECPEMReader.readECPrivateKey(aliceKeyFile)

        val bobKeyFile =
            File("src/test/resources/bob_ec_private.pem").bufferedReader()
        bobPrivateKey = ECPEMReader.readECPrivateKey(bobKeyFile)
    }

    @Test
    fun testRoot() = testApplication {
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Hello World!", bodyAsText())
        }
    }

    @Test
    fun testConnect() = testApplication {
        val packet = Packet.connect(alicePublicKey, ZonedDateTime.now())
        val json = Json.encodeToString(packet)
        val signature = base64Encoder.encodeToString(
            ECDSAContent(json.toByteArray()).signWith(alicePrivateKey)
        )

        client.config { install(WebSockets) }.webSocket("/ws") {
            send("$json;$signature")
            for (frame in incoming) {
                val text = if (frame is Frame.Text) frame.readText() else ""
                assertEquals("successful", text)
                close(CloseReason(CloseReason.Codes.NORMAL, "done"))
            }
            val reason = closeReason.await()
            assertEquals(
                CloseReason.Codes.NORMAL,
                CloseReason.Codes.values().first { it.code == reason?.code })
        }
    }

    @Test
    fun testSignal() = testApplication {
        val alice = client.config { install(WebSockets) }
        val bob = client.config { install(WebSockets) }
        val randomData = base64Encoder.encodeToString(
            Random(Date().time.toInt()).nextBytes(80)
        )

        val now = ZonedDateTime.now()

        val alicePacket = Packet.connect(alicePublicKey, now)
        val aliceJson = Json.encodeToString(alicePacket)
        val aliceSignature = base64Encoder.encodeToString(
            ECDSAContent(aliceJson.toByteArray()).signWith(alicePrivateKey)
        )

        val bobSignalPacket = Packet.signal(alicePublicKey, randomData, now)
        val bobSignalJson = Json.encodeToString(bobSignalPacket)
        val bobSignalSignature = base64Encoder.encodeToString(
            ECDSAContent(bobSignalJson.toByteArray()).signWith(
                bobPrivateKey
            )
        )
        val aliceReceivedPacket = Packet.signal(bobPublicKey, randomData, now)
        val aliceReceivedJson = Json.encodeToString(aliceReceivedPacket)

        runBlocking {
            launch {
                alice.webSocket("/ws") {
                    send("$aliceJson;$aliceSignature")
                    incoming.consumeAsFlow().withIndex().onEach {
                        val frame = it.value
                        val text =
                            if (frame is Frame.Text) frame.readText() else ""

                        if (it.index == 1) {
                            close(CloseReason(CloseReason.Codes.NORMAL, "done"))
                            assertEquals(
                                "$aliceReceivedJson;$bobSignalSignature",
                                text
                            )
                        }
                    }
                }
            }

            val bobConnectPacket = Packet.connect(bobPublicKey, now)
            val bobConnectJson = Json.encodeToString(bobConnectPacket)
            val bobConnectSignature = base64Encoder.encodeToString(
                ECDSAContent(bobConnectJson.toByteArray()).signWith(
                    bobPrivateKey
                )
            )

            bob.webSocket("/ws") {
                send("$bobConnectJson;$bobConnectSignature")
                incoming.consumeAsFlow().withIndex().onEach {
                    val frame = it.value
                    val text = if (frame is Frame.Text) frame.readText() else ""
                    send("$bobSignalJson;$bobSignalSignature")

                    if (it.index == 1) {
                        close(CloseReason(CloseReason.Codes.NORMAL, "done"))
                        assertEquals("request sent", text)
                    }
                }
            }
        }
    }

    @Test
    fun testSignalFailed() = testApplication {
        val bob = client.config { install(WebSockets) }

        bob.webSocket("/ws") {
            send("CNT:$bobPublicKey:$bobSignature")
            incoming.consumeAsFlow().withIndex().onEach {
                val frame = it.value
                val text = if (frame is Frame.Text) frame.readText() else ""

                when (it.index) {
                    0 -> send("$alicePublicKey:$bobSignature:SDP")
                    1 -> {
                        close(CloseReason(CloseReason.Codes.NORMAL, "done"))
                        assertEquals("target not found", text)
                    }
                }
            }
        }
    }

    @Test
    fun testConnectFailed() = testApplication {
        val bob = client.config { install(WebSockets) }
        val randomSignature = Base64
            .getEncoder()
            .encodeToString(Random(Date().time.toInt()).nextBytes(80))

        bob.webSocket("/ws") {
            send("CNT:$alicePublicKey:$randomSignature")
            val reason = closeReason.await()
            assertEquals(
                CloseReason.Codes.CANNOT_ACCEPT,
                CloseReason.Codes.values().first { it.code == reason?.code })
        }

        bob.webSocket("/ws") {
            send("$alicePublicKey:$bobSignature:SDP")
            val reason = closeReason.await()
            assertEquals(
                CloseReason.Codes.CANNOT_ACCEPT,
                CloseReason.Codes.values().first { it.code == reason?.code })
        }
    }
}
