package com.example.mizukisync

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class WebViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        val webView = findViewById<WebView>(R.id.webView)
        // 🌟 核心配置：必须开启 JS 和 DomStorage
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                Log.d("MizukiOAuth", "正在加载: $url")

                // 🌟 拦截逻辑：匹配你的回调地址
                if (url.startsWith("https://api.mizuki.top/auth/callback")) {
                    val code = request?.url?.getQueryParameter("code")
                    if (code != null) {
                        // 拿到 Code 后立刻返回给 LoginActivity
                        val resultIntent = Intent().apply { putExtra("oauth_code", code) }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                    return true
                }
                return false
            }
        }

        // 🌟 使用你提供的正确链接 (已包含正确的 Scope)
        val authUrl = "https://maimai.lxns.net/oauth/authorize?response_type=code&client_id=c9dc866a-1d49-4159-beb0-b711f19055ac&redirect_uri=https%3A%2F%2Fapi.mizuki.top%2Fauth%2Fcallback&scope=read_user_profile+read_player+write_player+read_user_token"
        webView.loadUrl(authUrl)
    }
}