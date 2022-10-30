package me.kohpai.crypto

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.PrivateKey
import java.security.Signature
import java.util.Base64

class ECDSAContent(private val message: ByteArray) {
    private val signer =
        Signature.getInstance("SHA256withECDSA", BouncyCastleProvider())

    fun signWith(privateKey: PrivateKey): String {
        signer.initSign(privateKey)
        signer.update(message)
        return Base64.getEncoder().encodeToString(signer.sign())
    }
}