package me.kohpai.plugins

import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/ws") { // websocketSession
            for (frame in incoming) {
                val message = if (frame is Frame.Text) frame.readText() else ""
                val chunks = message.split(':')
                val command = chunks.firstOrNull() ?: ""
                val response = if (command == "CNT") "successful" else "failed"

                outgoing.send(Frame.Text(response))

                close(
                    CloseReason(
                        CloseReason.Codes.NORMAL,
                        "$response connection"
                    )
                )
            }
        }
    }
}
