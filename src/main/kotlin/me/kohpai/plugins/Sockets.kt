package me.kohpai.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.kohpai.Command
import me.kohpai.Packet
import me.kohpai.connections
import me.kohpai.crypto.ECDSASignature
import me.kohpai.crypto.ECPEMReader
import org.bouncycastle.util.encoders.DecoderException
import java.io.StringReader
import java.security.SignatureException
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Base64

fun Application.configureSockets() {
    val app = this

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
                val chunks = message.split(';')
                if (chunks.size != 2) {
                    close(
                        CloseReason(
                            CloseReason.Codes.CANNOT_ACCEPT,
                            "not connected"
                        )
                    )
                    continue
                }

                val json = chunks[0]
                val packet = Json.decodeFromString<Packet>(json)
                val signature = chunks[1]

                when (packet.cmd) {
                    Command.CNT -> connection =
                        handleConnection(packet, json, signature)

                    Command.SGN -> {
                        if (connection != null) {
                            handleSignaling(connection, packet, signature)
                        } else {
                            close(
                                CloseReason(
                                    CloseReason.Codes.CANNOT_ACCEPT,
                                    "not connected"
                                )
                            )
                        }
                    }
                }
            }

            if (connection != null) {
                connections.remove(connection)
            }
            app.log.info("current connections: ${connections.size}")
        }
    }
}

fun isValidTimeDifference(dateTime: ZonedDateTime): Boolean {
    val now = ZonedDateTime.now()
    val diff = ChronoUnit.SECONDS.between(dateTime, now)

    return diff in 0..60
}

suspend fun DefaultWebSocketServerSession.handleConnection(
    packet: Packet,
    json: String,
    signature: String,
): String? {
    val dateTime = packet.signedAt
    if (!isValidTimeDifference(dateTime)) {
        close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "stale timestamp"))
        return null
    }

    val publicKeyPem = packet.pubKey
    val validSignature = try {
        val publicKey = ECPEMReader.readECPublicKey(StringReader(publicKeyPem))

        ECDSASignature(
            Base64
                .getDecoder()
                .decode(signature)
        ).verifyWith(json.toByteArray(), publicKey)
    } catch (e: DecoderException) {
        false
    } catch (e: SignatureException) {
        false
    }

    if (!validSignature) {
        close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "invalid signature"))
        return null
    }

    connections[publicKeyPem] = this
    outgoing.send(Frame.Text("successful"))
    return publicKeyPem
}

suspend fun DefaultWebSocketServerSession.handleSignaling(
    from: String,
    packet: Packet,
    signature: String,
) {
    val sending = Packet.signal(from, packet.data!!, packet.signedAt)
    val sendingJson = Json.encodeToString(sending)
    val connection = connections[packet.pubKey]
    if (connection != null) {
        connection.outgoing.send(Frame.Text("$sendingJson;$signature"))
        outgoing.send(Frame.Text("request sent"))
    } else {
        outgoing.send(Frame.Text("target not found"))
    }
}