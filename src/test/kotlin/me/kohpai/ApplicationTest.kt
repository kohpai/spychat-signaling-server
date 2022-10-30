package me.kohpai

import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import me.kohpai.crypto.ECDSAContent
import me.kohpai.crypto.ECPEMReader
import java.io.BufferedReader
import java.io.File
import java.util.Base64
import java.util.Date
import java.util.stream.Collectors
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
        val publicKeyFile =
            File("src/test/resources/ec_public.pem").bufferedReader()
        val privateKeyFile =
            File("src/test/resources/ec_private.pem").bufferedReader()
        val publicKey = ECPEMReader.readECPublicKey(publicKeyFile)
        val privateKey = ECPEMReader.readECPrivateKey(privateKeyFile)

        val publicKeyPem = trimPem(publicKeyFile)
        val signature = ECDSAContent(publicKey.encoded).signWith(privateKey)

        assertEquals(0, connections.size)

        client.config {
            install(WebSockets)
        }.webSocket("/ws") {
            send("CNT:$publicKeyPem:$signature")
            for (frame in incoming) {
                val text = if (frame is Frame.Text) frame.readText() else ""
                assertEquals("successful", text)
                close(CloseReason(CloseReason.Codes.NORMAL, "Test done"))
            }
        }

        assertEquals(1, connections.size)
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
        val publicKeyFile =
            File("src/test/resources/ec_public.pem").bufferedReader()
        val publicKeyPem = trimPem(publicKeyFile)
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

fun trimPem(pem: BufferedReader): String {
    val lines = pem.lines().map { it.trim() }.collect(Collectors.toList())
    return lines.slice(1..lines.size - 2).joinToString("")
}
