package com.example.mizukisync.network

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // !!! 核心修改：直接写死你的 CF Tunnel 域名 !!!
    // 注意：必须以 / 结尾
    private const val BASE_URL = "https://api.mizuki.top/"

    private var retrofit: Retrofit? = null

    fun getInstance(context: Context): ApiService {
        if (retrofit == null) {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                // 自动带上 Token
                .addInterceptor { chain ->
                    val original = chain.request()
                    val prefs = context.getSharedPreferences("mizuki_prefs", Context.MODE_PRIVATE)
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
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!.create(ApiService::class.java)
    }
}