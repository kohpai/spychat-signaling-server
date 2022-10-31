package me.kohpai

import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import java.util.Base64
import java.util.Date
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Hello World!", bodyAsText())
        }
    }

    @Test
    fun testConnectSuccessful() = testApplication {
        val publicKeyPem = "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEWYsCG1SsWZlYJT8yV" +
                "1TVJIgkpxTYliWKAgL5Eotx2cX6bAVlX+G4folAl5q6fo/fcq1B4QKaWkHBO" +
                "DXz5J+yPa1s1gnIwCqxpdo0nqAd9JmEJPxO0oaNTk8nZSnObQVe"
        val signature = "MGUCMAx/4E4+cTHl8C1/MpaY5UwWhJpive1SS+nEGM34wvfAwnM2" +
                "wzjIc4Jx/kCasHkc2QIxAJWvTx0jHhyYsB7yxdvSsg+D9DtyLTC4lgsNl5w1" +
                "bFXhxDigt2Jzqd5M7oX5/3SE5w=="

        assertEquals(0, connections.size)

        client.config {
            install(WebSockets)
        }.webSocket("/ws") {
            send("CNT:$publicKeyPem:$signature")
            for (frame in incoming) {
                val text = if (frame is Frame.Text) frame.readText() else ""
                assertEquals("successful", text)
                assertEquals(1, connections.size)
                close(CloseReason(CloseReason.Codes.NORMAL, "Test done"))
            }
        }

        assertEquals(0, connections.size)
    }

    @Test
    fun testConnectFailedByCommand() = testApplication {
        client.config {
            install(WebSockets)
        }.webSocket("/ws") {
            send("SGN:pub_key:SDP")
            for (frame in incoming) {
                val text = if (frame is Frame.Text) frame.readText() else ""
                assertEquals("failed", text)
                close(CloseReason(CloseReason.Codes.NORMAL, "Test done"))
            }
        }
    }

    @Test
    fun testConnectFailedBySignature() = testApplication {
        val publicKeyPem = "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEWYsCG1SsWZlYJT8yV" +
                "1TVJIgkpxTYliWKAgL5Eotx2cX6bAVlX+G4folAl5q6fo/fcq1B4QKaWkHBO" +
                "DXz5J+yPa1s1gnIwCqxpdo0nqAd9JmEJPxO0oaNTk8nZSnObQVe"
        val randomSignature = Base64
            .getEncoder()
            .encodeToString(Random(Date().time.toInt()).nextBytes(80))

        client.config {
            install(WebSockets)
        }.webSocket("/ws") {
            send("CNT:$publicKeyPem:$randomSignature")
            for (frame in incoming) {
                val text = if (frame is Frame.Text) frame.readText() else ""
                assertEquals("failed", text)
                close(CloseReason(CloseReason.Codes.NORMAL, "Test done"))
            }
        }
    }
}
