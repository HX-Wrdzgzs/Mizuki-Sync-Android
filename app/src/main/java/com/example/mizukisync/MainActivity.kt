package com.example.mizukisync

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.mizukisync.network.UnsafeOkHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var ivAvatar: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var tvTrophy: TextView
    private lateinit var tvDan: TextView
    private lateinit var tvClass: TextView
    private lateinit var tvStar: TextView
    private lateinit var tvFriendCode: TextView
    private lateinit var tvRating: TextView

    private lateinit var ivBgPlate: ImageView
    private lateinit var ivFrame: ImageView

    private val client by lazy { UnsafeOkHttpClient.getClient() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ivAvatar = findViewById(R.id.iv_avatar)
        tvUsername = findViewById(R.id.tv_username)
        tvTrophy = findViewById(R.id.tv_trophy)
        tvDan = findViewById(R.id.tv_dan)
        tvClass = findViewById(R.id.tv_class)
        tvStar = findViewById(R.id.tv_star)
        tvFriendCode = findViewById(R.id.tv_friend_code)
        tvRating = findViewById(R.id.tv_rating)

        ivBgPlate = findViewById(R.id.iv_bg_plate)
        ivFrame = findViewById(R.id.iv_frame)

        setupGridButtons()
        fetchUserProfile()
    }

    private fun setupGridButtons() {
        findViewById<View>(R.id.btn_song_search).setOnClickListener {
            startActivity(Intent(this, SongSearchActivity::class.java))
        }
        findViewById<View>(R.id.btn_refresh).setOnClickListener {
            fetchUserProfile()
        }
        findViewById<View>(R.id.btn_logout_grid).setOnClickListener {
            logout()
        }
        findViewById<View>(R.id.btn_score_query).setOnClickListener {
            Toast.makeText(this, "成绩查询功能开发中...", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.btn_bind_df).setOnClickListener {
            Toast.makeText(this, "绑定功能开发中...", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.btn_tools).setOnClickListener {
            Toast.makeText(this, "工具箱开发中...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchUserProfile() {
        val prefs = getSharedPreferences("MizukiPrefs", Context.MODE_PRIVATE)
        // 🌟 核心修复：统一使用 "token" 这个钥匙来开箱！
        val token = prefs.getString("token", "") ?: ""

        if (token.isEmpty()) {
            Toast.makeText(this, "提示：尚未绑定 Token，请先登录！", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "正在同步玩家数据...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url("https://api.mizuki.top/api/user/profile")
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                val response = client.newCall(request).execute()
                val responseData = response.body()?.string()

                if (response.isSuccessful && responseData != null) {
                    val json = JSONObject(responseData)
                    withContext(Dispatchers.Main) { updateUI(json) }

                } else if (response.code() == 401) {
                    // 抓到 401 过期信号，立刻启动无感续命！
                    doSilentRefresh()

                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "获取数据失败: HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "网络异常: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 🌟 终极自动续命引擎
    private suspend fun doSilentRefresh() {
        val prefs = getSharedPreferences("MizukiPrefs", Context.MODE_PRIVATE)
        val refreshToken = prefs.getString("refresh_token", "") ?: ""

        if (refreshToken.isEmpty()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "身份凭证丢失，请重新登录", Toast.LENGTH_LONG).show()
                logout()
            }
            return
        }

        try {
            // 构建一个空请求体的 POST 请求
            val emptyBody = RequestBody.create(null, ByteArray(0))
            val request = Request.Builder()
                .url("https://api.mizuki.top/api/oauth/refresh?refresh_token=$refreshToken")
                .post(emptyBody)
                .build()

            val response = client.newCall(request).execute()
            val responseData = response.body()?.string()

            if (response.isSuccessful && responseData != null) {
                val json = JSONObject(responseData)

                val newToken = json.optString("token", "")
                val newRefresh = json.optString("refresh_token", "")

                prefs.edit()
                    // 🌟 核心修复：续命成功后，保存的新钥匙也要叫 "token"！
                    .putString("token", newToken)
                    .putString("refresh_token", if (newRefresh.isNotEmpty()) newRefresh else refreshToken)
                    .apply()

                withContext(Dispatchers.Main) {
                    updateUI(json)
                    Toast.makeText(this@MainActivity, "✨ 登录已过期，但已为您在后台自动续签！", Toast.LENGTH_SHORT).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "长期未活跃，身份已彻底过期，请重新授权", Toast.LENGTH_LONG).show()
                    logout()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "静默刷新网络异常: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(data: JSONObject) {
        tvUsername.text = data.optString("username", "Unknown")
        tvRating.text = data.optInt("maimai_rating", 0).toString()
        tvFriendCode.text = "ID: ${data.optString("friend_code", "------")}"
        tvTrophy.text = data.optString("trophy", "初出茅庐")
        tvDan.text = data.optString("dan", "初学者")
        tvClass.text = data.optString("class_rank", "B5")
        tvStar.text = "★×${data.optInt("star", 0)}"

        val iconUrl = data.optString("icon_url", "")
        if (iconUrl.isNotEmpty()) {
            Glide.with(this).load(iconUrl).into(ivAvatar)
        }
    }

    private fun logout() {
        getSharedPreferences("MizukiPrefs", Context.MODE_PRIVATE).edit().clear().apply()
        Toast.makeText(this, "已退出当前账号", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}