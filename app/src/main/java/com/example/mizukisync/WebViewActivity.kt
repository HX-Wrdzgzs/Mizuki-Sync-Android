package com.example.mizukisync

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
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

        // 暴力清除缓存，确保每次都重新登录
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        webView.clearCache(true)
        webView.clearHistory()

        // 使用你提供的官方 URL，确保包含 read_user_token
        val authUrl = "https://maimai.lxns.net/oauth/authorize?response_type=code&client_id=c9dc866a-1d49-4159-beb0-b711f19055ac&redirect_uri=https%3A%2F%2Fapi.mizuki.top%2Fauth%2Fcallback&scope=read_user_profile+read_player+write_player+read_user_token"

        val targetRedirectUri = "https://api.mizuki.top/auth/callback"

        webView.webViewClient = object : WebViewClient() {
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