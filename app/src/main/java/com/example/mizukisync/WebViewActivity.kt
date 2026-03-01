package com.example.mizukisync

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        webView = WebView(this)
        setContentView(webView)

        setupModernWebView()

        // 🌟 修复点：去掉了强制清理 Cookie 的代码！
        // 现在你的 WebView 拥有了记忆，只要你在落雪网页里登录过一次，以后进来直接点“授权”就行了！

        val oauthUrl = Uri.parse("https://maimai.lxns.net/oauth/authorize").buildUpon()
            .appendQueryParameter("client_id", "c9dc866a-1d49-4159-beb0-b711f19055ac")
            .appendQueryParameter("redirect_uri", "https://api.mizuki.top/auth/callback")
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", "read_user_profile read_player")
            .appendQueryParameter("state", "mizuki_sync_safe")
            .build()
            .toString()

        webView.loadUrl(oauthUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupModernWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false

                if (url.startsWith("https://api.mizuki.top/auth/callback")) {
                    val code = request.url.getQueryParameter("code")

                    if (!code.isNullOrEmpty()) {
                        Toast.makeText(this@WebViewActivity, "授权成功！正在移交后端...", Toast.LENGTH_SHORT).show()

                        val resultIntent = Intent()
                        resultIntent.putExtra("oauth_code", code)
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    } else {
                        Toast.makeText(this@WebViewActivity, "授权失败或被取消", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    return true
                }
                return false
            }
        }
        webView.webChromeClient = WebChromeClient()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}