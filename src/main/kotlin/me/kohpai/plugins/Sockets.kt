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
                            "failed connection"
                        )
                    )
                    continue
                }

                if (chunks[0] == "CNT") {
                    connection = handleConnection(chunks[1], chunks[2])
                } else {
                    handleSignaling(chunks[0])
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
    var response = "failed"
    if (validSignature) {
        connection = publicKeyPem
        response = "successful"
        connections[connection] = this
    }

    outgoing.send(Frame.Text(response))
    return connection
}

suspend fun DefaultWebSocketServerSession.handleSignaling(target: String) {
    val connection = connections[target]
    if (connection != null) {
        outgoing.send(Frame.Text("request sent"))
        connection.outgoing.send(Frame.Text("chat requested"))
    } else {
        outgoing.send(Frame.Text("target not found"))
    }
}