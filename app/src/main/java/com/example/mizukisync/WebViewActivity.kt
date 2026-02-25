package com.example.mizukisync

import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class WebViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val webView = WebView(this)
        setContentView(webView)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        // 暴力清除缓存
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        webView.clearCache(true)
        webView.clearHistory()

        // 🌟 核心杀招：把 redirect_uri 原封不动地加回来，并且用最原始的字符串拼接，不让 Android 自动转码！
        val authUrl = "https://maimai.lxns.net/oauth/authorize" +
                "?response_type=code" +
                "&client_id=c9dc866a-1d49-4159-beb0-b711f19055ac" +
                "&redirect_uri=https://api.mizuki.top/auth/callback" +
                "&scope=read_user_profile%20read_player" // 简化 scope，只请求最必要的基础权限

        val targetRedirectUri = "https://api.mizuki.top/auth/callback"

        webView.webViewClient = object : WebViewClient() {

            // 强行放行 SSL 证书错误
            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                handler?.proceed()
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()

                if (url.startsWith(targetRedirectUri)) {
                    val uri = Uri.parse(url)
                    val code = uri.getQueryParameter("code")
                    if (code != null) {
                        val result = Intent()
                        result.putExtra("code", code)
                        setResult(RESULT_OK, result)
                        finish()
                        return true
                    }
                }
                return false
            }
        }

        webView.loadUrl(authUrl)
    }
}