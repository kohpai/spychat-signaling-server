package me.kohpai

import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Base64
import java.util.Date
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    private val alicePublicKey =
        "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEWYsCG1SsWZlYJT8yV1TVJIgkpxTYliWKAgL5Eotx2cX6bAVlX+G4folAl5q6fo/fcq1B4QKaWkHBODXz5J+yPa1s1gnIwCqxpdo0nqAd9JmEJPxO0oaNTk8nZSnObQVe"
    private val aliceSignature =
        "MGUCMAx/4E4+cTHl8C1/MpaY5UwWhJpive1SS+nEGM34wvfAwnM2wzjIc4Jx/kCasHkc2QIxAJWvTx0jHhyYsB7yxdvSsg+D9DtyLTC4lgsNl5w1bFXhxDigt2Jzqd5M7oX5/3SE5w=="
    private val bobPublicKey =
        "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEECK5wO0XZQR654QS00UFKxTVNcD72ESaq9JTOtEB8XI3imxIiCHQih7aymBGZESKYKamy8bR9vwiBK87o0IEykFrNkQE5T1lchipihb6tfrhet3CH5C/7z3nJmiFtSu/"
    private val bobSignature =
        "MGQCMHAcjskdykriWxwstMhSAmXsVo6pBFGDmLyPxz54D1GThDdDOLECsa4lGZAnel49tAIwTGoQjk3iw3H/bA7EQ2sUncKOJqPxPVwtercmEEYX1DM0LXCmmh1pZtTUdyaRL3CQ"

    @Test
    fun testRoot() = testApplication {
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Hello World!", bodyAsText())
        }
    }

    @Test
    fun testConnect() = testApplication {
        assertEquals(0, connections.size)

        client.config {
            install(WebSockets)
        }.webSocket("/ws") {
            send("CNT:$alicePublicKey:$aliceSignature")
            for (frame in incoming) {
                val text = if (frame is Frame.Text) frame.readText() else ""
                assertEquals("successful", text)
                assertEquals(1, connections.size)
                close(CloseReason(CloseReason.Codes.NORMAL, "done"))
            }
        }

        assertEquals(0, connections.size)
    }

    @Test
    fun testSignal() = testApplication {
        val alice = client.config {
            install(WebSockets)
        }
        val bob = client.config { install(WebSockets) }

        runBlocking {
            launch {
                alice.webSocket("/ws") {
                    send("CNT:$alicePublicKey:$aliceSignature")
                    var counter = 0
                    for (frame in incoming) {
                        val text =
                            if (frame is Frame.Text) frame.readText() else ""
                        if (counter++ == 1) {
                            assertEquals("chat requested", text)
                            close(CloseReason(CloseReason.Codes.NORMAL, "done"))
                        }
                    }
                }
            }

            bob.webSocket("/ws") {
                send("CNT:$bobPublicKey:$bobSignature")
                var counter = 0
                for (frame in incoming) {
                    val text = if (frame is Frame.Text) frame.readText() else ""

                    when (counter++) {
                        0 -> send("$alicePublicKey:$bobSignature:SDP")
                        1 -> {
                            assertEquals("request sent", text)
                            close(CloseReason(CloseReason.Codes.NORMAL, "done"))
                        }
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
            var counter = 0
            for (frame in incoming) {
                val text = if (frame is Frame.Text) frame.readText() else ""

                when (counter++) {
                    0 -> send("$alicePublicKey:$bobSignature:SDP")
                    1 -> {
                        assertEquals("target not found", text)
                        close(CloseReason(CloseReason.Codes.NORMAL, "done"))
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
