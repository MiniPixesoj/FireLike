package com.pixesoj.freefirelike.utils

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class CryptoUtils {
    companion object {
        fun encryptAES(data: ByteArray): ByteArray {
            val key = "Yg&tc%DEuh6%Zc^8".toByteArray()
            val iv = "6oyZDr22E3ychjM%".toByteArray()
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val secretKeySpec = SecretKeySpec(key, "AES")
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec)
            return cipher.doFinal(data)
        }
    }
}