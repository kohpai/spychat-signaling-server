package me.kohpai

import me.kohpai.crypto.ECPEMReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

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

}