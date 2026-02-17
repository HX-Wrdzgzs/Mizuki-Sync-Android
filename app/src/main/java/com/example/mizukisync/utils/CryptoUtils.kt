package com.example.mizukisync.utils

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

object CryptoUtils {
    // 确保这个 Key 和后端 main.py 里的一模一样！
    private const val SECRET_KEY = "Mizuki_Nightcord_Secret_2500"

    fun sign(path: String, timestamp: String): String {
        try {
            val message = "$path$timestamp"
            val sha256_HMAC = Mac.getInstance("HmacSHA256")
            val secret_key = SecretKeySpec(SECRET_KEY.toByteArray(), "HmacSHA256")
            sha256_HMAC.init(secret_key)
            val bytes = sha256_HMAC.doFinal(message.toByteArray())
            // 使用 NO_WRAP 避免换行符导致签名不匹配
            return Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }
}