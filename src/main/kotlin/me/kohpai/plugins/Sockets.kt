package me.kohpai.plugins

import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import io.ktor.server.application.*
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
            var connection: String? = null

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

                val publicKeyPem = chunks[1]
                val publicKey =
                    ECPEMReader.readECPublicKey(StringReader(publicKeyPem))
                val validSignature = ECDSASignature(
                    Base64
                        .getDecoder()
                        .decode(chunks[2])
                ).verifyWith(publicKey.encoded, publicKey)

                val validConnection = chunks.first() == "CNT" && validSignature
                if (validConnection) {
                    connection = publicKeyPem
                    connections[publicKeyPem] = this
                }

                val response = if (validConnection) "successful" else "failed"
                outgoing.send(Frame.Text(response))
            }

            if (connection != null) {
                connections.remove(connection)
            }
        }
    }
}
