package me.kohpai.plugins

import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import io.ktor.server.application.*
import me.kohpai.Connection
import me.kohpai.connections
import me.kohpai.crypto.ECDSASignature
import me.kohpai.crypto.ECPEMReader
import java.io.StringReader
import java.util.Base64

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
                if (chunks.size != 3) {
                    close(
                        CloseReason(
                            CloseReason.Codes.NORMAL,
                            "failed connection"
                        )
                    )
                    continue
                }

                val publicKey =
                    ECPEMReader.readECPublicKey(StringReader(chunks[1]))
                val validSignature = ECDSASignature(
                    Base64
                        .getDecoder()
                        .decode(chunks[2])
                ).verifyWith(publicKey.encoded, publicKey)

                val validConnection = chunks.first() == "CNT" && validSignature
                if (validConnection) {
                    connections.add(Connection(chunks[1], this))
                }

                val response = if (validConnection) "successful" else "failed"
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
