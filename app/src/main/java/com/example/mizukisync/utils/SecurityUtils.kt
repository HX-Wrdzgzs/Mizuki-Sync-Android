package com.example.mizukisync.utils

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {
    private const val ALGORITHM = "AES/CBC/PKCS7Padding"
    private const val KEY = "YOUR_32_CHAR_KEY_HERE____" // 必须与 Python 后端一致
    private const val IV = "YOUR_16_CHAR_IV__"         // 必须与 Python 后端一致

    // AES 加密：发送请求前使用
    fun encrypt(data: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        val keySpec = SecretKeySpec(KEY.toByteArray(), "AES")
        val ivSpec = IvParameterSpec(IV.toByteArray())
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        return Base64.encodeToString(cipher.doFinal(data.toByteArray()), Base64.NO_WRAP)
    }

    // 动态签名生成：防止请求被伪造
    fun generateSign(timestamp: String): String {
        val raw = timestamp + KEY
        return java.security.MessageDigest.getInstance("MD5")
            .digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}