package com.example.mizukisync

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.mizukisync.network.UnsafeOkHttpClient
import kotlinx.coroutines.*
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class SongDetailActivity : AppCompatActivity() {

    private val DEV_API_KEY = "gAtzZcA6iXdihYhBtbw8VeXUtnFsMUI-Iwdyd-_ZvKM=" //
    private val client by lazy { UnsafeOkHttpClient.getUnsafeOkHttpClient() }

    private var difficultiesArray: JSONArray? = null
    private var songId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_detail)

        // 1. 绑定返回按钮
        findViewById<View>(R.id.btn_back_circle).setOnClickListener { finish() }

        songId = intent.getIntExtra("song_id", 0)
        if (songId != 0) {
            fetchFullData(songId)
        } else {
            Toast.makeText(this, "无效的乐曲 ID", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun fetchFullData(id: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 并行获取详情和个人最高成绩
                val detailTask = async { fetchSongDetail(id) }
                val scoreTask = async { fetchMyScore(id) }
                detailTask.await()
                scoreTask.await()
            } catch (e: Exception) { Log.e("MizukiSync", "Data fetch failed", e) }
        }
    }

    private suspend fun fetchSongDetail(id: Int) {
        val url = "https://api.mizuki.top/api/songs/search?id=$id"
        val resp = client.newCall(Request.Builder().url(url).build()).execute()
        if (resp.isSuccessful) {
            val root = JSONObject(resp.body?.string() ?: "")
            val song = root.optJSONArray("data")?.optJSONObject(0) ?: return

            // 🌟 修正：解析 standard 嵌套数组
            difficultiesArray = song.optJSONObject("difficulties")?.optJSONArray("standard")

            withContext(Dispatchers.Main) {
                bindBaseInfo(song)
                updateDifficultyUI(3) // 默认进入选中 Master
            }
        }
    }

    private suspend fun fetchMyScore(id: Int) {
        val prefs = getSharedPreferences("MizukiPrefs", Context.MODE_PRIVATE)
        val cache = prefs.getString("user_profile_cache", "") ?: ""
        val fCode = if (cache.isNotEmpty()) JSONObject(cache).optString("friend_code", "") else ""

        if (fCode.isNotEmpty()) {
            // 🌟 核心：使用友码请求落雪历史记录并按 song_id 过滤
            val url = "https://maimai.lxns.net/api/v0/maimai/player/$fCode/score/history?song_id=$id"
            val request = Request.Builder().url(url).addHeader("Authorization", DEV_API_KEY).build()
            val resp = client.newCall(request).execute()
            if (resp.isSuccessful) {
                val data = JSONObject(resp.body?.string() ?: "").optJSONArray("data")
                if (data != null && data.length() > 0) {
                    val best = data.getJSONObject(0) // 第一条通常是最新/最高分
                    withContext(Dispatchers.Main) { updateScoreUI(best) }
                }
            }
        }
    }

    private fun bindBaseInfo(song: JSONObject) {
        //
        findViewById<TextView>(R.id.tv_detail_title).text = song.optString("title")
        findViewById<TextView>(R.id.tv_detail_artist).text = song.optString("artist")
        findViewById<TextView>(R.id.tv_info_id).text = song.optString("id")
        findViewById<TextView>(R.id.tv_info_bpm).text = song.optString("bpm")
        findViewById<TextView>(R.id.tv_info_version).text = song.optString("version")
        findViewById<TextView>(R.id.tv_info_category).text = song.optString("genre", "maimai")

        val jacketUrl = "https://assets2.lxns.net/maimai/jacket/${songId}.png"
        Glide.with(this).load(jacketUrl).into(findViewById(R.id.iv_header_bg))
        Glide.with(this).load(jacketUrl).into(findViewById(R.id.iv_detail_jacket_pop))

        // 难度选择按钮
        val btnIds = arrayOf(R.id.btn_diff_0, R.id.btn_diff_1, R.id.btn_diff_2, R.id.btn_diff_3, R.id.btn_diff_4)
        btnIds.forEachIndexed { index, id ->
            findViewById<TextView>(id).setOnClickListener { updateDifficultyUI(index) }
        }
    }

    private fun updateDifficultyUI(index: Int) {
        val diffs = difficultiesArray ?: return
        if (index >= diffs.length()) return
        val diffObj = diffs.getJSONObject(index)

        // 更新定数与设计师
        findViewById<TextView>(R.id.tv_detail_level).text = diffObj.optString("level")
        findViewById<TextView>(R.id.tv_detail_const).text = String.format("%.1f", diffObj.optDouble("level_value", 0.0))
        findViewById<TextView>(R.id.tv_detail_designer).text = diffObj.optString("note_designer", "-")

        // 更新 Note 统计
        val n = diffObj.optJSONObject("notes")
        findViewById<TextView>(R.id.tv_detail_total).text = n?.optString("total", "-")
        findViewById<TextView>(R.id.tv_detail_tap).text = n?.optString("tap", "-")
        findViewById<TextView>(R.id.tv_detail_hold).text = n?.optString("hold", "-")
        findViewById<TextView>(R.id.tv_detail_slide).text = n?.optString("slide", "-")
        findViewById<TextView>(R.id.tv_detail_touch).text = n?.optString("touch", "-")
        findViewById<TextView>(R.id.tv_detail_break).text = n?.optString("break", "-")

        // 切换高亮
        val tabs = findViewById<LinearLayout>(R.id.layout_diff_tabs)
        for (i in 0 until tabs.childCount) {
            val v = tabs.getChildAt(i) as? TextView
            v?.setBackgroundColor(if (i == index) Color.WHITE else Color.TRANSPARENT)
        }
    }

    private fun updateScoreUI(score: JSONObject) {
        // 🌟 修复：填充底部成绩面板
        findViewById<TextView>(R.id.tv_score_achievement).text = "${String.format("%.4f", score.optDouble("achievement", 0.0))}%"
        findViewById<TextView>(R.id.tv_score_rate).text = score.optString("rate", "??")
        findViewById<TextView>(R.id.tv_score_dx).text = "DX Score: ${score.optInt("dx_score", 0)}"

        val fc = score.optString("fc", "")
        if (fc.isNotEmpty()) {
            findViewById<TextView>(R.id.tv_score_fc).visibility = View.VISIBLE
            findViewById<TextView>(R.id.tv_score_fc).text = fc.uppercase()
        }
    }
}