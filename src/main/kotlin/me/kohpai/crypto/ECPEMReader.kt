package me.kohpai.crypto

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.File
import java.io.Reader
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey

class ECPEMReader {
    companion object {
        private val keyConverter = JcaPEMKeyConverter()

        fun readECPrivateKey(reader: Reader): ECPrivateKey {
            val privateKeyFile =
                File("src/test/resources/ec_private.pem").bufferedReader()
            val privateKeyPem = PEMParser(privateKeyFile).readObject()
            val keyPair = keyConverter.getKeyPair(privateKeyPem as PEMKeyPair)

            return keyPair.private as ECPrivateKey
        }

        fun readECPublicKey(reader: Reader): ECPublicKey {
            val publicKeyFile =
                File("src/test/resources/ec_public.pem").bufferedReader()
            val publicKeyPem = PEMParser(publicKeyFile).readObject()

            return keyConverter.getPublicKey(publicKeyPem as SubjectPublicKeyInfo) as ECPublicKey
        }
    }
}
