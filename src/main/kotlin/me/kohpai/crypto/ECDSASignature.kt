package me.kohpai.crypto

import java.security.PublicKey
import java.security.Signature

class ECDSASignature(private val signature: ByteArray) {
    private val signer =
        Signature.getInstance("SHA256withECDSA")

    fun verifyWith(content: ByteArray, publicKey: PublicKey): Boolean {
        signer.initVerify(publicKey)
        signer.update(content)
        return signer.verify(signature)
    }
}