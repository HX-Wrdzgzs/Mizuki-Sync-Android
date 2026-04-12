package com.example.mizukisync.network

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

class AuthInterceptor(private val context: Context) : Interceptor {
    private val SECRET_KEY = "Mizuki_Nightcord_Secret_2500"

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val prefs = context.getSharedPreferences("MizukiPrefs", Context.MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""

        // 🌟 核心修正：去掉 ()
        val path = originalRequest.url.encodedPath
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val signature = calculateSign(path, timestamp)

        val newRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .header("X-Mizuki-Time", timestamp)
            .header("X-Mizuki-Sign", signature)
            .method(originalRequest.method, originalRequest.body) // 🌟 核心修正：去掉 ()
            .build()

        return chain.proceed(newRequest)
    }

    private fun calculateSign(path: String, timestamp: String): String {
        return try {
            val message = "$path$timestamp"
            val sha256_HMAC = Mac.getInstance("HmacSHA256")
            val secretKey = SecretKeySpec(SECRET_KEY.toByteArray(), "HmacSHA256")
            sha256_HMAC.init(secretKey)
            val bytes = sha256_HMAC.doFinal(message.toByteArray())
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) { "" }
    }
}