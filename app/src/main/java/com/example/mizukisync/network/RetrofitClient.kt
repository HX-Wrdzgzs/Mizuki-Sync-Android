package com.example.mizukisync.network

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // 默认地址 (仅作保底)
    private const val DEFAULT_URL = "http://10.0.2.2:8000"

    private var retrofit: Retrofit? = null
    private var currentBaseUrl: String? = null

    fun getInstance(context: Context): ApiService {
        // 1. 从缓存读取用户设置的 IP
        val prefs = context.getSharedPreferences("mizuki_prefs", Context.MODE_PRIVATE)
        // 注意：这里必须保证 URL 以 / 结尾，Retrofit 强制要求
        var baseUrl = prefs.getString("server_url", DEFAULT_URL) ?: DEFAULT_URL
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/"
        }

        // 2. 如果地址变了，或者还没初始化，就重新构建 Retrofit
        if (retrofit == null || baseUrl != currentBaseUrl) {
            currentBaseUrl = baseUrl

            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val original = chain.request()
                    val token = prefs.getString("access_token", null)

                    val requestBuilder = original.newBuilder()
                    if (!token.isNullOrEmpty()) {
                        requestBuilder.header("Authorization", "Bearer $token")
                    }
                    val request = requestBuilder.build()
                    chain.proceed(request)
                }
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl) // 使用动态读取的 IP
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!.create(ApiService::class.java)
    }
}