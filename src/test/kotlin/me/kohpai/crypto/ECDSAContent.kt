package me.kohpai.crypto

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.PrivateKey
import java.security.Signature

class ECDSAContent(private val content: ByteArray) {
    private val signer =
        Signature.getInstance("SHA256withECDSA", BouncyCastleProvider())

    fun signWith(privateKey: PrivateKey): ByteArray {
        signer.initSign(privateKey)
        signer.update(content)
        return signer.sign()
    }
}