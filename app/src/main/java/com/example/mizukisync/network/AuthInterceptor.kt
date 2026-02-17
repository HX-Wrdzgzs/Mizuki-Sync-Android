package com.example.mizukisync.network

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

class AuthInterceptor(private val context: Context) : Interceptor {

    // 必须与后端 main.py 保持一致
    private val SECRET_KEY = "Mizuki_Nightcord_Secret_2500"

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 1. 动态获取 Token
        val prefs = context.getSharedPreferences("mizuki_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("access_token", "") ?: ""

        // 2. 准备加密参数
        // 修复点：添加 encodedPath() 括号，适配 OkHttp 3.x Java 接口
        val path = originalRequest.url().encodedPath()
        val timestamp = (System.currentTimeMillis() / 1000).toString()

        // 3. 计算签名 (直接在这里算，不依赖外部文件，解决找不到 sign 的问题)
        val signature = calculateSign(path, timestamp)

        // 4. 重建请求
        // 修复点：使用 .method() 和 .body() 方法调用
        val newRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .header("X-Mizuki-Time", timestamp)
            .header("X-Mizuki-Sign", signature)
            .method(originalRequest.method(), originalRequest.body())
            .build()

        return chain.proceed(newRequest)
    }

    // 内部私有加密方法
    private fun calculateSign(path: String, timestamp: String): String {
        return try {
            val message = "$path$timestamp"
            val sha256_HMAC = Mac.getInstance("HmacSHA256")
            val secret_key = SecretKeySpec(SECRET_KEY.toByteArray(), "HmacSHA256")
            sha256_HMAC.init(secret_key)
            val bytes = sha256_HMAC.doFinal(message.toByteArray())
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}