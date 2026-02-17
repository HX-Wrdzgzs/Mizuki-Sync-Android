package com.example.mizukisync.network

import android.annotation.SuppressLint
import android.content.Context
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@SuppressLint("StaticFieldLeak") // 忽略 Context 泄漏警告，作为单例持有 Application Context 是安全的
object RetrofitClient {
    private const val BASE_URL = "https://api.mizuki.top/"
    private var retrofit: Retrofit? = null

    // 修改：你需要先调用 init 才能用
    fun getInstance(context: Context): ApiService {
        if (retrofit == null) {

            // 1. 配置 SSL 证书锁定
            val certificatePinner = CertificatePinner.Builder()
                .add("api.mizuki.top", "sha256/HWRijKO9kd0zFhWZGFV1E+QAC9Zfit5UczDMM96Wihs=")
                .build()

            // 2. 创建 Client，把 Application Context 传给拦截器
            val client = OkHttpClient.Builder()
                .certificatePinner(certificatePinner)
                .addInterceptor(AuthInterceptor(context.applicationContext))
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