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

                val pem = "-----BEGIN PUBLIC KEY-----\n" +
                        "${chunks[1]}\n" +
                        "-----END PUBLIC KEY-----"
                if (chunks[0] == "CNT") {
                    connection = handleConnection(pem, chunks[2])
                } else {
                    close(
                        CloseReason(
                            CloseReason.Codes.CANNOT_ACCEPT,
                            "failed connection"
                        )
                    )
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
        val publicKey = ECPEMReader.readECPublicKey(StringReader(publicKeyPem))

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