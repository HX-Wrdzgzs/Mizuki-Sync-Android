package com.example.mizukisync

import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class WebViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 创建 WebView
        val webView = WebView(this)
        setContentView(webView)

        // 2. 核心配置 (解决白屏问题)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true // 必开！否则 Vue/React 页面会白屏
            useWideViewPort = true
            loadWithOverviewMode = true
            databaseEnabled = true
        }

        // 3. 清除旧的登录状态 (解决无法切换账号问题)
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()

        // 4. 设置拦截逻辑
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()

                // 拦截回调：一旦检测到 api.mizuki.top，说明登录成功
                if (url.startsWith("https://api.mizuki.top/auth/callback")) {
                    val code = request?.url?.getQueryParameter("code")
                    if (code != null) {
                        val resultIntent = Intent()
                        resultIntent.putExtra("code", code)
                        setResult(RESULT_OK, resultIntent)
                        finish() // 关闭浏览器，回 App
                        return true
                    }
                }
                return false // 其他链接正常加载
            }
        }

        // 5. 加载页面
        title = "正在连接落雪..."
        val authUrl = "https://maimai.lxns.net/oauth/authorize?response_type=code&client_id=c9dc866a-1d49-4159-beb0-b711f19055ac&redirect_uri=https://api.mizuki.top/auth/callback&scope=read_user_profile+write_player+read_player+read_user_token"
        webView.loadUrl(authUrl)
    }
}