package com.example.mizukisync

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class WebViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val webView = WebView(this)
        setContentView(webView)

        // 允许 Cookie 和 Storage，防止重复登录问题
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        // 你的配置
        val clientId = "c9dc866a-1d49-4159-beb0-b711f19055ac"
        val redirectUri = "https://api.mizuki.top/auth/callback"

        // !!! 核心修复：必须申请 read_player 权限，中间用空格分隔 !!!
        // 如果这里不加 read_player，后端查 /player/me 必挂
        val scope = "read_user_profile read_player"

        // 手动构造 URL，空格转义为 %20
        val authUrl = "https://maimai.lxns.net/oauth/authorize?response_type=code&client_id=$clientId&redirect_uri=$redirectUri&scope=read_user_profile%20read_player"

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()

                // 拦截回调
                if (url.startsWith(redirectUri)) {
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

        // 清除缓存，强制让用户重新授权，确保拿到新权限
        webView.clearCache(true)
        webView.clearHistory()

        webView.loadUrl(authUrl)
    }
}