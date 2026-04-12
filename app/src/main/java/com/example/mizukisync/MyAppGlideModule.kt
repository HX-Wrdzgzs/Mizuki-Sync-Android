package com.example.mizukisync

import android.content.Context
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.example.mizukisync.network.UnsafeOkHttpClient
import java.io.InputStream

@GlideModule
class MyAppGlideModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        // 🌟 诊断日志：如果在 Logcat 搜不到这一行，说明这个类没生效
        Log.e("MizukiSync", "GlideModule 正在初始化：已挂载 SSL 绕过插件")

        val client = UnsafeOkHttpClient.getUnsafeOkHttpClient()
        registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(client))
    }
}