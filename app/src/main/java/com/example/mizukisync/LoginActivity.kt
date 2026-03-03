package com.example.mizukisync

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.mizukisync.network.UnsafeOkHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private val webViewLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val code = result.data?.getStringExtra("oauth_code")
            if (!code.isNullOrEmpty()) {
                Toast.makeText(this, "拿到授权码！正在呼叫大后方...", Toast.LENGTH_SHORT).show()
                exchangeCodeForToken(code)
            }
        } else {
            Toast.makeText(this, "授权已取消", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        val prefs = getSharedPreferences("MizukiPrefs", Context.MODE_PRIVATE)
        val savedToken = prefs.getString("token", "")
        if (!savedToken.isNullOrEmpty()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        val loginBtn = findViewById<View>(R.id.btn_login)
        if (loginBtn != null) {
            loginBtn.setOnClickListener {
                val intent = Intent(this, WebViewActivity::class.java)
                webViewLauncher.launch(intent)
            }
        } else {
            Toast.makeText(this, "警告：找不到 ID 为 btn_login 的控件", Toast.LENGTH_LONG).show()
        }
    }

    private fun exchangeCodeForToken(code: String) {
        val client = UnsafeOkHttpClient.getClient()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://api.mizuki.top/api/oauth/token?code=$code"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val jsonString = response.body()?.string() ?: ""

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && jsonString.isNotEmpty()) {
                        val json = JSONObject(jsonString)

                        // 🌟 修复点：兼容两种命名，并同时提取 15分钟短牌 和 30天长牌！
                        val token = json.optString("token", json.optString("access_token", ""))
                        val refreshToken = json.optString("refresh_token", "")
                        val username = json.optString("username", "玩家")

                        if (token.isNotEmpty()) {
                            val prefs = getSharedPreferences("MizukiPrefs", Context.MODE_PRIVATE)
                            prefs.edit()
                                .putString("token", token)                 // 存入 15分钟 的门禁卡
                                .putString("refresh_token", refreshToken)  // 🌟 救命稻草：把 30天 的 VIP 金牌死死攥在手里！
                                .putString("username", username)
                                .apply()

                            Toast.makeText(this@LoginActivity, "欢迎回来，${username}！", Toast.LENGTH_LONG).show()
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this@LoginActivity, "登录失败: 后端未返回 Token", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, "后端大门紧闭: HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "网络异常: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}