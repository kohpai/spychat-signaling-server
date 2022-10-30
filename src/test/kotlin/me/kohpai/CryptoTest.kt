package me.kohpai

import me.kohpai.crypto.ECDSASignature
import me.kohpai.crypto.ECPEMReader
import java.io.File
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class CryptoTest {
    @Test
    fun testReadPrivatePemKeys() {
        val privateKeyFile =
            File("src/test/resources/ec_private.pem").bufferedReader()
        val privateKey = ECPEMReader.readECPrivateKey(privateKeyFile)

        assertEquals(384, privateKey.params.curve.field.fieldSize)
    }

    @Test
    fun testReadPublicPemKeys() {
        val publicKeyFile =
            File("src/test/resources/ec_public.pem").bufferedReader()
        val publicKey = ECPEMReader.readECPublicKey(publicKeyFile)

        assertEquals(384, publicKey.params.curve.field.fieldSize)
    }

    @Test
    fun testVerifySignature() {
        val publicKeyFile =
            File("src/test/resources/ec_public.pem").bufferedReader()
        val publicKey = ECPEMReader.readECPublicKey(publicKeyFile)
        val signature = Base64
            .getDecoder()
            .decode(
                "MGUCMQDANmnoyS1q/kv3f/x1aZsERdps8g5lUJeD6jjI+f4EPgHR3ZE3" +
                        "wuC+ILuSNYCm1XACMDRjAkWrmdSJN2h2R5ihw5AUXJEs+MwSLQ" +
                        "p9GYeYK7zj+avcGw10/BFmIqa25Hn8gQ=="
            )
        assertTrue {
            ECDSASignature(signature).verifyWith(
                publicKey.encoded,
                publicKey
            )
        }

        assertFails {
            ECDSASignature(
                signature
                    .slice(1..signature.size - 2)
                    .toByteArray()
            ).verifyWith(publicKey.encoded, publicKey)
        }
    }

}