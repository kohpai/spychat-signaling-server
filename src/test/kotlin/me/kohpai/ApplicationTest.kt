package me.kohpai

import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.File
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
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
    fun testReadPrivatePemKeys() {
        val keyConverter = JcaPEMKeyConverter()

        val privateKeyFile =
            File("src/test/resources/ec_private.pem").bufferedReader()
        val privateKeyPem = PEMParser(privateKeyFile).readObject()
        val keyPair = keyConverter.getKeyPair(privateKeyPem as PEMKeyPair)
        val privateKey = keyPair.private as ECPrivateKey

        assertEquals(384, privateKey.params.curve.field.fieldSize)
    }

    @Test
    fun testReadPublicPemKeys() {
        val keyConverter = JcaPEMKeyConverter()

        val publicKeyFile =
            File("src/test/resources/ec_public.pem").bufferedReader()
        val publicKeyPem = PEMParser(publicKeyFile).readObject()
        val publicKey =
            keyConverter.getPublicKey(publicKeyPem as SubjectPublicKeyInfo) as ECPublicKey

        assertEquals(384, publicKey.params.curve.field.fieldSize)
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