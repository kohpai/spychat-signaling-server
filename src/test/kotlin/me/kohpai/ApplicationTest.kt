package me.kohpai

import io.ktor.client.plugins.websocket.*
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlin.test.*
import io.ktor.server.testing.*
import me.kohpai.plugins.*

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
        client.config {
            install(WebSockets)
        }.webSocket("/ws") {
            send("CNT:pub_key:signature")
            for (frame in incoming) {
                val text = if (frame is Frame.Text) frame.readText() else ""
                assertEquals("successful", text)
                close(CloseReason(CloseReason.Codes.NORMAL, "Test done"))
            }
        }
    }

    @Test
    fun testConnectFailed() = testApplication {
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
}