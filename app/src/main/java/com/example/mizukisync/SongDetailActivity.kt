package com.example.mizukisync

import android.content.Context
import android.graphics.Color
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
import org.json.JSONArray
import org.json.JSONObject

class SongDetailActivity : AppCompatActivity() {

    private var diffArray: JSONArray? = null
    private val tabIds = listOf(R.id.btn_diff_0, R.id.btn_diff_1, R.id.btn_diff_2, R.id.btn_diff_3, R.id.btn_diff_4)
    // BSC, ADV, EXP, MAS, Re:M 的选中高亮底色
    private val tabColors = listOf("#A5D6A7", "#FFF59D", "#EF9A9A", "#CE93D8", "#B3E5FC")

    // 🌟 核心：用来存储当前这首歌的成绩字典！Key 是难度索引 (0~4)
    private val myScoreMap = mutableMapOf<Int, JSONObject>()
    private var currentDiffIndex = -1
    private var currentSongId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_song_detail)

        findViewById<View>(R.id.btn_back_circle)?.setOnClickListener { finish() }

        val songJsonStr = intent.getStringExtra("song_data") ?: ""
        if (songJsonStr.isEmpty()) {
            Toast.makeText(this, "数据传输失败", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        try {
            val song = JSONObject(songJsonStr)
            currentSongId = song.optString("id", "")
            setupUI(song)

            // 🌟 绝杀：页面一打开，立刻拿着你的 Token 去落雪服务器把这首歌的成绩拉回来！
            if (currentSongId.isNotEmpty()) {
                fetchMyScores(currentSongId)
            }
        } catch (e: Throwable) {
            Toast.makeText(this, "数据解析异常: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupUI(song: JSONObject) {
        // 1. 填充基础歌曲信息
        findViewById<TextView>(R.id.tv_detail_title)?.text = song.optString("title", "未知曲目")
        findViewById<TextView>(R.id.tv_detail_artist)?.text = song.optString("artist", "未知曲师")
        findViewById<TextView>(R.id.tv_detail_bpm)?.text = song.optInt("bpm", 0).toString()
        findViewById<TextView>(R.id.tv_detail_id)?.text = "#${song.optString("id", "???")}"

        val basicInfo = song.optJSONObject("basic_info")
        findViewById<TextView>(R.id.tv_detail_version)?.text = basicInfo?.optString("version", "未知版本")
        findViewById<TextView>(R.id.tv_detail_category)?.text = basicInfo?.optString("genre", "未知类别")

        val jacketUrl = song.optString("jacket_url", "")
        findViewById<ImageView>(R.id.iv_header_bg)?.let { Glide.with(this).load(jacketUrl).into(it) }
        findViewById<ImageView>(R.id.iv_detail_jacket_pop)?.let { Glide.with(this).load(jacketUrl).into(it) }

        // 2. 提取难度数组 (优先取 DX 谱，如果没有 DX 谱就取标准谱)
        val diffObj = song.optJSONObject("difficulties")
        diffArray = diffObj?.optJSONArray("dx")
        if (diffArray == null || diffArray?.length() == 0) {
            diffArray = diffObj?.optJSONArray("standard")
        }

        // 3. 配置难度切换标签页
        if (diffArray != null && diffArray!!.length() > 0) {
            val length = diffArray!!.length()
            // 如果只有 4 个难度，就把白谱(Re:M)的按钮隐藏掉
            if (length < 5) findViewById<TextView>(R.id.btn_diff_4)?.visibility = View.GONE
            else findViewById<TextView>(R.id.btn_diff_4)?.visibility = View.VISIBLE

            for (i in 0 until length) {
                findViewById<TextView>(tabIds[i])?.setOnClickListener { selectDifficulty(i) }
            }
            // 默认选中最高难度
            selectDifficulty(length - 1)
        }
    }

    // 🌟 切页逻辑：点击不同难度按钮时，瞬间刷新数据面板！
    private fun selectDifficulty(index: Int) {
        val array = diffArray ?: return
        if (index >= array.length()) return
        currentDiffIndex = index

        // 1. 更新按钮颜色反馈
        for (i in tabIds.indices) {
            val btn = findViewById<TextView>(tabIds[i])
            if (i == index) btn?.setBackgroundColor(Color.parseColor(tabColors[i]))
            else btn?.setBackgroundColor(Color.TRANSPARENT)
        }

        // 2. 更新谱面硬核数据
        val targetDiff = array.getJSONObject(index)
        findViewById<TextView>(R.id.tv_detail_level)?.text = targetDiff.optString("level", "?")
        findViewById<TextView>(R.id.tv_detail_const)?.text = targetDiff.optString("level_value", "-.-")
        findViewById<TextView>(R.id.tv_detail_designer)?.text = targetDiff.optString("note_designer", "未知")

        val notesObj = targetDiff.optJSONObject("notes")
        if (notesObj != null) {
            findViewById<TextView>(R.id.tv_detail_tap)?.text = notesObj.optInt("tap", 0).toString()
            findViewById<TextView>(R.id.tv_detail_hold)?.text = notesObj.optInt("hold", 0).toString()
            findViewById<TextView>(R.id.tv_detail_slide)?.text = notesObj.optInt("slide", 0).toString()
            findViewById<TextView>(R.id.tv_detail_touch)?.text = notesObj.optInt("touch", 0).toString()
            findViewById<TextView>(R.id.tv_detail_break)?.text = notesObj.optInt("break", 0).toString()
        }

        // 3. 刷新下方的玩家真实成绩面板
        refreshScorePanel()
    }

    // 🌟 将成绩数据渲染到界面的核心函数
    private fun refreshScorePanel() {
        val scoreObj = myScoreMap[currentDiffIndex]

        val tvAchievement = findViewById<TextView>(R.id.tv_score_achievement)
        val tvRate = findViewById<TextView>(R.id.tv_score_rate)
        val tvDx = findViewById<TextView>(R.id.tv_score_dx)
        val tvFc = findViewById<TextView>(R.id.tv_score_fc)
        val tvFs = findViewById<TextView>(R.id.tv_score_fs)

        if (scoreObj == null) {
            // 这首歌的这个难度你还没打过
            tvAchievement?.text = "0.0000%"
            tvRate?.text = "UNPLAYED"
            tvRate?.setTextColor(Color.parseColor("#94A3B8")) // 灰色
            tvDx?.text = "DX Score: 0"
            tvFc?.visibility = View.GONE
            tvFs?.visibility = View.GONE
        } else {
            // 打过！把落雪服务器传回来的数据贴上去
            val achievements = scoreObj.optDouble("achievements", 0.0)
            tvAchievement?.text = String.format("%.4f%%", achievements)

            val rate = scoreObj.optString("rate", "CLEAR").uppercase()
            tvRate?.text = rate

            // 🌟 极其细节的颜色评级系统！
            when {
                rate.startsWith("SSS") -> tvRate?.setTextColor(Color.parseColor("#FBBF24")) // SSS金
                rate.startsWith("SS") -> tvRate?.setTextColor(Color.parseColor("#F472B6"))  // SS粉
                rate.startsWith("S") -> tvRate?.setTextColor(Color.parseColor("#FCD34D"))   // S黄
                else -> tvRate?.setTextColor(Color.parseColor("#4ADE80"))                   // 绿
            }

            tvDx?.text = "DX Score: ${scoreObj.optInt("dx_score", 0)}"

            // 检查有没有打出 FC / AP (全连/全P) 牌子
            val fc = scoreObj.optString("fc", "")
            if (fc.isNotEmpty() && fc != "null") {
                tvFc?.visibility = View.VISIBLE
                tvFc?.text = " ${fc.uppercase()} "
            } else {
                tvFc?.visibility = View.GONE
            }

            // 检查有没有打出 FS / FDX (同步) 牌子
            val fs = scoreObj.optString("fs", "")
            if (fs.isNotEmpty() && fs != "null") {
                tvFs?.visibility = View.VISIBLE
                tvFs?.text = " ${fs.uppercase()} "
            } else {
                tvFs?.visibility = View.GONE
            }
        }
    }

    // 🌟 终极请求引擎：拿着你的 Token 直连 LXNS 获取玩家成绩！
    private fun fetchMyScores(targetSongId: String) {
        val prefs = getSharedPreferences("MizukiPrefs", Context.MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""

        if (token.isEmpty()) return

        val client = UnsafeOkHttpClient.getClient()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 直接向落雪官方的查分 API 要数据！
                val request = Request.Builder()
                    .url("https://maimai.lxns.net/api/v0/user/maimai/player/scores")
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                val response = client.newCall(request).execute()
                val responseData = response.body()?.string()

                if (response.isSuccessful && responseData != null) {
                    val json = JSONObject(responseData)
                    val dataArray = json.optJSONArray("data") ?: JSONArray()

                    // 因为返回的是你所有的成绩，我们需要在客户端进行过滤
                    // 把所有属于这首歌的成绩挑出来，按照难度(level_index)存进字典里！
                    for (i in 0 until dataArray.length()) {
                        val scoreItem = dataArray.getJSONObject(i)
                        // 注意：为了防止类型不匹配，这里强制转换为 String 来对比
                        if (scoreItem.optString("id") == targetSongId) {
                            val levelIndex = scoreItem.optInt("level_index", -1)
                            if (levelIndex != -1) {
                                myScoreMap[levelIndex] = scoreItem
                            }
                        }
                    }

                    // 数据拿到了，切换回主线程，刷新界面！
                    withContext(Dispatchers.Main) {
                        refreshScorePanel()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}