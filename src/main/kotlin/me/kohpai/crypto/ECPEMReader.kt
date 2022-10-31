package me.kohpai.crypto

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.Reader
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey

class ECPEMReader {
    companion object {
        private val keyConverter = JcaPEMKeyConverter()

        fun readECPrivateKey(reader: Reader): ECPrivateKey {
            val privateKeyPem = PEMParser(reader).readObject()
            val keyPair = keyConverter.getKeyPair(privateKeyPem as PEMKeyPair)

            return keyPair.private as ECPrivateKey
        }

        fun readECPublicKey(reader: Reader): ECPublicKey {
            val publicKeyPem = PEMParser(reader).readObject()

            return keyConverter.getPublicKey(publicKeyPem as SubjectPublicKeyInfo) as ECPublicKey
        }
    }
}
