package me.kohpai.plugins

import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import io.ktor.server.application.*
import me.kohpai.connections
import me.kohpai.crypto.ECDSASignature
import me.kohpai.crypto.ECPEMReader
import org.bouncycastle.util.encoders.DecoderException
import java.io.StringReader
import java.security.SignatureException
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
                            CloseReason.Codes.CANNOT_ACCEPT,
                            "not connected"
                        )
                    )
                    continue
                }

                if (chunks[0] == "CNT") {
                    connection = handleConnection(chunks[1], chunks[2])
                } else if (connection == null) {
                    close(
                        CloseReason(
                            CloseReason.Codes.CANNOT_ACCEPT,
                            "not connected"
                        )
                    )
                } else {
                    handleSignaling(connection, chunks)
                }
            }

            if (connection != null) {
                connections.remove(connection)
            }
        }
    }
}

suspend fun DefaultWebSocketServerSession.handleConnection(
    publicKeyPem: String,
    signature: String
): String? {
    val validSignature = try {
        val pem =
            "-----BEGIN PUBLIC KEY-----\n$publicKeyPem\n-----END PUBLIC KEY-----"
        val publicKey = ECPEMReader.readECPublicKey(StringReader(pem))

        ECDSASignature(
            Base64
                .getDecoder()
                .decode(signature)
        ).verifyWith(publicKey.encoded, publicKey)
    } catch (e: DecoderException) {
        false
    } catch (e: SignatureException) {
        false
    }

    var connection: String? = null
    if (validSignature) {
        connection = publicKeyPem
        connections[connection] = this
        outgoing.send(Frame.Text("successful"))
    } else {
        close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "invalid signature"))
    }

    return connection
}

suspend fun DefaultWebSocketServerSession.handleSignaling(
    from: String,
    packet: List<String>
) {
    val connection = connections[packet[0]]
    if (connection != null) {
        connection.outgoing.send(Frame.Text("$from:${packet[1]}:${packet[2]}"))
        outgoing.send(Frame.Text("request sent"))
    } else {
        outgoing.send(Frame.Text("target not found"))
    }
}