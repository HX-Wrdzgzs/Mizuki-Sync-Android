package com.example.mizukisync

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.mizukisync.network.UnsafeOkHttpClient
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private val DEV_API_KEY = "gAtzZcA6iXdihYhBtbw8VeXUtnFsMUI-Iwdyd-_ZvKM=" //

    private lateinit var ivAvatar: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var tvTrophy: TextView
    private lateinit var tvRating: TextView
    private lateinit var ivRecentJacket: ImageView
    private lateinit var tvRecentTitle: TextView
    private lateinit var tvRecentAchievement: TextView
    private lateinit var tvRecentDiff: TextView

    private val client by lazy { UnsafeOkHttpClient.getUnsafeOkHttpClient() }
    private var currentFriendCode: String = ""
    private var randomSongId: Int = 0 // 🌟 用于点击跳转

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. 控件绑定
        ivAvatar = findViewById(R.id.iv_avatar)
        tvUsername = findViewById(R.id.tv_username)
        tvTrophy = findViewById(R.id.tv_trophy)
        tvRating = findViewById(R.id.tv_rating)
        ivRecentJacket = findViewById(R.id.iv_recent_jacket)
        tvRecentTitle = findViewById(R.id.tv_recent_title)
        tvRecentAchievement = findViewById(R.id.tv_recent_achievement)
        tvRecentDiff = findViewById(R.id.tv_recent_diff)

        tvTrophy.isSelected = true

        // 🌟 2. 绑定 6 个核心按钮点击事件
        findViewById<View>(R.id.btn_song_search).setOnClickListener { startActivity(Intent(this, SongSearchActivity::class.java)) }

        findViewById<View>(R.id.btn_refresh).setOnClickListener {
            Toast.makeText(this, "正在向落雪同步并抽取随机成绩...", Toast.LENGTH_SHORT).show()
            syncWithLXNS()
        }

        findViewById<View>(R.id.btn_score_query).setOnClickListener { startActivity(Intent(this, B50Activity::class.java)) }
        findViewById<View>(R.id.btn_bind_df).setOnClickListener { startActivity(Intent(this, WebViewActivity::class.java)) }
        findViewById<View>(R.id.btn_tools).setOnClickListener { Toast.makeText(this, "实用工具模块开发中...", Toast.LENGTH_SHORT).show() }
        findViewById<View>(R.id.btn_logout_grid).setOnClickListener { performLogout() }

        // 🌟 3. 核心功能：点击随机成绩卡片进入详情
        findViewById<View>(R.id.card_recent).setOnClickListener {
            if (randomSongId != 0) {
                val intent = Intent(this, SongDetailActivity::class.java)
                intent.putExtra("song_id", randomSongId)
                startActivity(intent)
            } else {
                Toast.makeText(this, "请先刷新数据以获取随机曲目", Toast.LENGTH_SHORT).show()
            }
        }

        loadCachedData()
        syncWithLXNS()
    }

    private fun loadCachedData() {
        val prefs = getSharedPreferences("MizukiPrefs", Context.MODE_PRIVATE)
        val cache = prefs.getString("user_profile_cache", "") ?: ""
        if (cache.isNotEmpty()) try { updateUI(JSONObject(cache)) } catch (e: Exception) {}
    }

    private fun syncWithLXNS() {
        val token = getSharedPreferences("MizukiPrefs", Context.MODE_PRIVATE).getString("token", "") ?: ""
        if (token.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 第一步：向后端请求好友码
                val resp = client.newCall(Request.Builder()
                    .url("https://api.mizuki.top/api/user/profile")
                    .addHeader("Authorization", "Bearer $token").build()).execute()

                val body = resp.body?.string() ?: ""
                if (resp.isSuccessful) {
                    getSharedPreferences("MizukiPrefs", Context.MODE_PRIVATE).edit().putString("user_profile_cache", body).apply()
                    withContext(Dispatchers.Main) {
                        val json = JSONObject(body)
                        updateUI(json)
                        // 第二步：根据好友码去落雪抓历史数据
                        if (currentFriendCode.isNotEmpty()) fetchRandomHistoryScore(currentFriendCode)
                    }
                }
            } catch (e: Exception) { Log.e("MizukiSync", "Sync Error", e) }
        }
    }

    private fun fetchRandomHistoryScore(fCode: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url("https://maimai.lxns.net/api/v0/maimai/player/$fCode/score/history")
                    .addHeader("Authorization", DEV_API_KEY)
                    .build()

                val resp = client.newCall(request).execute()
                val body = resp.body?.string() ?: ""
                if (resp.isSuccessful) {
                    val root = JSONObject(body)
                    val dataArray = root.optJSONArray("data")
                    if (dataArray != null && dataArray.length() > 0) {
                        // 🌟 随机算法：抽签
                        val randomIndex = Random.nextInt(dataArray.length())
                        val randomScore = dataArray.getJSONObject(randomIndex)
                        withContext(Dispatchers.Main) {
                            updateRecentUI(randomScore)
                            Toast.makeText(this@MainActivity, "成功获取随机成绩并同步状态", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun updateUI(data: JSONObject) {
        tvUsername.text = data.optString("username", data.optString("name", "Unknown"))
        tvRating.text = data.optInt("maimai_rating", data.optInt("rating", 0)).toString()
        tvTrophy.text = data.optString("trophy", data.optString("title", "落雪查分器用户"))
        currentFriendCode = data.optString("friend_code", data.optString("code", ""))
        findViewById<TextView>(R.id.tv_friend_code).text = "ID: $currentFriendCode"
        findViewById<TextView>(R.id.tv_star).text = "★×${data.optInt("star", 0)}"

        val icon = data.optString("icon_url", "")
        if (icon.isNotEmpty()) Glide.with(this).load(icon).into(ivAvatar)
    }

    private fun updateRecentUI(score: JSONObject) {
        val song = score.optJSONObject("song")
        tvRecentTitle.text = song?.optString("title") ?: score.optString("song_name", "未知曲目")
        tvRecentAchievement.text = "${String.format("%.4f", score.optDouble("achievement", 0.0))}%"

        val diffNum = score.optInt("difficulty", 3)
        val diffNames = arrayOf("Basic", "Advanced", "Expert", "Master", "Re:Master")
        tvRecentDiff.text = diffNames.getOrElse(diffNum) { "MASTER" }.uppercase()

        // 🌟 记录 ID 用于跳转详情页
        randomSongId = score.optInt("song_id", song?.optInt("id") ?: 0)

        if (randomSongId != 0) {
            val jacketUrl = "https://maimai.lxns.net/api/v0/maimai/song/$randomSongId/jacket"
            Glide.with(this).load(jacketUrl).into(ivRecentJacket)
        }
    }

    private fun performLogout() {
        getSharedPreferences("MizukiPrefs", Context.MODE_PRIVATE).edit().clear().apply()
        Toast.makeText(this, "账号已退出，本地缓存已清除", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}