package me.kohpai

import me.kohpai.crypto.ECDSAContent
import me.kohpai.crypto.ECDSASignature
import me.kohpai.crypto.ECPEMReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class CryptoTest {
    @Test
    fun testReadPrivatePemKeys() {
        val privateKeyFile =
            File("src/test/resources/alice_ec_private.pem").bufferedReader()
        val privateKey = ECPEMReader.readECPrivateKey(privateKeyFile)

        assertEquals(384, privateKey.params.curve.field.fieldSize)
    }

    @Test
    fun testReadPublicPemKeys() {
        val publicKeyFile =
            File("src/test/resources/alice_ec_public.pem").bufferedReader()
        val publicKey = ECPEMReader.readECPublicKey(publicKeyFile)

        assertEquals(384, publicKey.params.curve.field.fieldSize)
    }

    @Test
    fun testVerifySignature() {
        val publicKeyFile =
            File("src/test/resources/alice_ec_public.pem").bufferedReader()
        val privateKeyFile =
            File("src/test/resources/alice_ec_private.pem").bufferedReader()
        val publicKey = ECPEMReader.readECPublicKey(publicKeyFile)
        val privateKey = ECPEMReader.readECPrivateKey(privateKeyFile)

        val content = publicKey.encoded
        val signature = ECDSAContent(content).signWith(privateKey)

//        val base64Encoder = Base64.getEncoder()
//        println(base64Encoder.encodeToString(content))
//        println(base64Encoder.encodeToString(signature))

        assertTrue {
            ECDSASignature(signature).verifyWith(
                content,
                publicKey
            )
        }

        assertFails {
            ECDSASignature(
                signature
                    .slice(1..signature.size - 2)
                    .toByteArray()
            ).verifyWith(content, publicKey)
        }
    }

}