package me.kohpai

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.File
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import kotlin.test.Test
import kotlin.test.assertEquals

class CryptoTest {
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

}