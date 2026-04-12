package com.example.mizukisync

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.mizukisync.network.UnsafeOkHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    // 🌟 处理授权页返回的结果
    private val webViewLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val code = result.data?.getStringExtra("oauth_code")
            if (!code.isNullOrEmpty()) {
                exchangeCodeForToken(code) // 去换取正式 Token
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        // 自动登录逻辑：如果有 Token 直接跳主页
        val prefs = getSharedPreferences("MizukiPrefs", Context.MODE_PRIVATE)
        if (!prefs.getString("token", "").isNullOrEmpty()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        findViewById<View>(R.id.btn_login).setOnClickListener {
            // 启动网页授权
            webViewLauncher.launch(Intent(this, WebViewActivity::class.java))
        }
    }

    private fun exchangeCodeForToken(code: String) {
        val client = UnsafeOkHttpClient.getUnsafeOkHttpClient()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 🌟 按照 LXNS 文档构造 POST 请求体
                val formBody = FormBody.Builder()
                    .add("grant_type", "authorization_code")
                    .add("client_id", "c9dc866a-1d49-4159-beb0-b711f19055ac")
                    .add("client_secret", "27IVVn6MgxtXxIN3gqpA8IbAKmix5AJl")
                    .add("code", code)
                    .add("redirect_uri", "https://api.mizuki.top/auth/callback")
                    .build()

                val request = Request.Builder()
                    .url("https://maimai.lxns.net/api/v0/oauth/token")
                    .post(formBody)
                    .build()

                val response = client.newCall(request).execute()
                val bodyString = response.body?.string() ?: ""

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        // LXNS 的响应数据在 data 字段下
                        val jsonData = JSONObject(bodyString).getJSONObject("data")
                        val token = jsonData.getString("access_token")

                        // 永久保存 Token
                        getSharedPreferences("MizukiPrefs", Context.MODE_PRIVATE).edit()
                            .putString("token", token)
                            .apply()

                        Toast.makeText(this@LoginActivity, "登录成功", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Log.e("MizukiOAuth", "换码失败: $bodyString")
                        Toast.makeText(this@LoginActivity, "换取令牌失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("MizukiOAuth", "网络异常", e)
            }
        }
    }
}