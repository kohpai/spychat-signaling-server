package me.kohpai.crypto

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.PrivateKey
import java.security.Signature

class ECDSAContent(private val message: ByteArray) {
    private val signer =
        Signature.getInstance("SHA256withECDSA", BouncyCastleProvider())

    fun signWith(privateKey: PrivateKey): ByteArray {
        signer.initSign(privateKey)
        signer.update(message)
        return signer.sign()
    }
}