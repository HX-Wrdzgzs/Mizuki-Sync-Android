package com.example.mizukisync

import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
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
    private val tabColors = listOf("#A5D6A7", "#FFF59D", "#EF9A9A", "#CE93D8", "#B3E5FC")

    private val borderGradients = listOf(
        intArrayOf(Color.parseColor("#81C784"), Color.parseColor("#2E7D32")), // 绿
        intArrayOf(Color.parseColor("#FFF176"), Color.parseColor("#F57F17")), // 黄
        intArrayOf(Color.parseColor("#E57373"), Color.parseColor("#C62828")), // 红
        intArrayOf(Color.parseColor("#BA68C8"), Color.parseColor("#6A1B9A")), // 紫
        intArrayOf(Color.parseColor("#E1BEE7"), Color.parseColor("#81D4FA"))  // 白紫
    )

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
            if (currentSongId.isNotEmpty()) fetchMyScores(currentSongId)
        } catch (e: Throwable) {
            Toast.makeText(this, "数据解析异常: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupUI(song: JSONObject) {
        findViewById<TextView>(R.id.tv_detail_title)?.text = song.optString("title", "未知曲目")
        findViewById<TextView>(R.id.tv_detail_artist)?.text = song.optString("artist", "未知曲师")

        findViewById<TextView>(R.id.tv_info_id)?.text = "#${song.optString("id", "???")}"
        findViewById<TextView>(R.id.tv_info_bpm)?.text = song.optInt("bpm", 0).toString()

        // 🌟 史诗级修复：暴力破解 Kotlin 的 Null 陷阱！
        val basicInfo = song.optJSONObject("basic_info")

        var versionVal = song.optString("version", "")
        if (versionVal.isEmpty() || versionVal == "null") versionVal = basicInfo?.optString("version", "") ?: ""
        if (versionVal.isEmpty() || versionVal == "null") versionVal = "未知"

        var categoryVal = song.optString("genre", "")
        if (categoryVal.isEmpty() || categoryVal == "null") categoryVal = basicInfo?.optString("genre", "") ?: ""
        if (categoryVal.isEmpty() || categoryVal == "null") categoryVal = "未知"

        // 🌟 终极翻译官：把冰冷的数字直接映射成绝美的版本代号！
        val versionMap = mapOf(
            "10000" to "maimai", "11000" to "maimai PLUS",
            "12000" to "GreeN", "13000" to "GreeN PLUS",
            "14000" to "ORANGE", "15000" to "ORANGE PLUS",
            "16000" to "PiNK", "17000" to "PiNK PLUS",
            "18000" to "MURASAKi", "18500" to "MURASAKi PLUS",
            "19000" to "MiLK", "19500" to "MiLK PLUS",
            "19900" to "FiNALE",
            "20000" to "maimai DX", "20500" to "maimai DX PLUS",
            "21000" to "Splash", "21500" to "Splash PLUS",
            "22000" to "UNiVERSE", "22500" to "UNiVERSE PLUS",
            "23000" to "FESTiVAL", "23500" to "FESTiVAL PLUS",
            "24000" to "BUDDiES", "24500" to "BUDDiES PLUS",
            "25000" to "PRiSM"
        )
        val displayVersion = versionMap[versionVal] ?: versionVal

        findViewById<TextView>(R.id.tv_info_version)?.text = displayVersion
        findViewById<TextView>(R.id.tv_info_category)?.text = categoryVal

        val jacketUrl = song.optString("jacket_url", "")
        findViewById<ImageView>(R.id.iv_header_bg)?.let { Glide.with(this).load(jacketUrl).into(it) }
        findViewById<ImageView>(R.id.iv_detail_jacket_pop)?.let { Glide.with(this).load(jacketUrl).into(it) }

        val diffObj = song.optJSONObject("difficulties")
        diffArray = diffObj?.optJSONArray("dx")
        if (diffArray == null || diffArray?.length() == 0) {
            diffArray = diffObj?.optJSONArray("standard")
        }

        if (diffArray != null && diffArray!!.length() > 0) {
            val length = diffArray!!.length()
            if (length < 5) findViewById<TextView>(R.id.btn_diff_4)?.visibility = View.GONE
            else findViewById<TextView>(R.id.btn_diff_4)?.visibility = View.VISIBLE

            for (i in 0 until length) {
                findViewById<TextView>(tabIds[i])?.setOnClickListener { selectDifficulty(i) }
            }
            selectDifficulty(length - 1)
        }
    }

    private fun selectDifficulty(index: Int) {
        val array = diffArray ?: return
        if (index >= array.length()) return
        currentDiffIndex = index

        val gradientBg = GradientDrawable(GradientDrawable.Orientation.TL_BR, borderGradients[index])
        gradientBg.cornerRadius = 60f
        findViewById<View>(R.id.layout_gradient_border)?.background = gradientBg

        for (i in tabIds.indices) {
            val btn = findViewById<TextView>(tabIds[i])
            if (i == index) btn?.setBackgroundColor(Color.parseColor(tabColors[i]))
            else btn?.setBackgroundColor(Color.TRANSPARENT)
        }

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

        refreshScorePanel()
    }

    private fun refreshScorePanel() {
        val scoreObj = myScoreMap[currentDiffIndex]
        val tvAchievement = findViewById<TextView>(R.id.tv_score_achievement)
        val tvRate = findViewById<TextView>(R.id.tv_score_rate)
        val tvDx = findViewById<TextView>(R.id.tv_score_dx)
        val tvFc = findViewById<TextView>(R.id.tv_score_fc)
        val tvFs = findViewById<TextView>(R.id.tv_score_fs)

        tvAchievement?.paint?.shader = null

        if (scoreObj == null) {
            tvAchievement?.text = "0.0000%"
            tvAchievement?.setTextColor(Color.parseColor("#94A3B8"))
            tvRate?.text = "未游玩"
            tvRate?.setTextColor(Color.parseColor("#94A3B8"))
            tvDx?.text = "DX Score: 0"
            tvFc?.visibility = View.GONE
            tvFs?.visibility = View.GONE
        } else {
            val achievements = scoreObj.optDouble("achievements", 0.0)
            tvAchievement?.text = String.format("%.4f%%", achievements)

            when {
                achievements < 80.0 -> tvAchievement?.setTextColor(Color.parseColor("#2196F3"))
                achievements < 97.0 -> tvAchievement?.setTextColor(Color.parseColor("#F48FB1"))
                achievements < 100.2 -> tvAchievement?.setTextColor(Color.parseColor("#FBC02D"))
                else -> {
                    tvAchievement?.setTextColor(Color.WHITE)
                    tvAchievement?.post {
                        val width = tvAchievement.paint.measureText(tvAchievement.text.toString())
                        val rainbowShader = LinearGradient(0f, 0f, width, 0f,
                            intArrayOf(
                                Color.parseColor("#FF5252"), Color.parseColor("#FFEB3B"),
                                Color.parseColor("#69F0AE"), Color.parseColor("#40C4FF"), Color.parseColor("#E040FB")
                            ), null, Shader.TileMode.CLAMP)
                        tvAchievement.paint.shader = rainbowShader
                        tvAchievement.invalidate()
                    }
                }
            }

            val rawRate = scoreObj.optString("rate", "").lowercase()
            val (rateStr, rateColor) = when (rawRate) {
                "sssp" -> "SSS+" to "#FBBF24"
                "sss" -> "SSS" to "#FBBF24"
                "ssp" -> "SS+" to "#F472B6"
                "ss" -> "SS" to "#F472B6"
                "sp" -> "S+" to "#FCD34D"
                "s" -> "S" to "#FCD34D"
                "aaa" -> "AAA" to "#4ADE80"
                "aa" -> "AA" to "#4ADE80"
                "a" -> "A" to "#4ADE80"
                else -> rawRate.uppercase() to "#94A3B8"
            }
            tvRate?.text = rateStr
            tvRate?.setTextColor(Color.parseColor(rateColor))

            tvDx?.text = "DX Score: ${scoreObj.optInt("dx_score", 0)}"

            val rawFc = scoreObj.optString("fc", "").lowercase()
            val fcStr = when (rawFc) {
                "app" -> "AP+"
                "ap" -> "AP"
                "fcp" -> "FC+"
                "fc" -> "FC"
                else -> ""
            }
            if (fcStr.isNotEmpty()) {
                tvFc?.visibility = View.VISIBLE
                tvFc?.text = " $fcStr "
            } else tvFc?.visibility = View.GONE

            val rawFs = scoreObj.optString("fs", "").lowercase()
            val fsStr = when (rawFs) {
                "fsdp" -> "FDX+"
                "fsd" -> "FDX"
                "fsp" -> "FS+"
                "fs" -> "FS"
                "sync" -> "SYNC PLAY"
                else -> ""
            }
            if (fsStr.isNotEmpty()) {
                tvFs?.visibility = View.VISIBLE
                tvFs?.text = " $fsStr "
            } else tvFs?.visibility = View.GONE
        }
    }

    private fun fetchMyScores(targetSongId: String) {
        val prefs = getSharedPreferences("MizukiPrefs", Context.MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""

        if (token.isEmpty()) return

        val client = UnsafeOkHttpClient.getClient()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url("https://maimai.lxns.net/api/v0/user/maimai/player/scores")
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                val response = client.newCall(request).execute()
                val responseData = response.body()?.string()

                if (response.isSuccessful && responseData != null) {
                    val json = JSONObject(responseData)
                    val dataArray = json.optJSONArray("data") ?: JSONArray()

                    for (i in 0 until dataArray.length()) {
                        val scoreItem = dataArray.getJSONObject(i)
                        if (scoreItem.optString("id") == targetSongId) {
                            val levelIndex = scoreItem.optInt("level_index", -1)
                            if (levelIndex != -1) myScoreMap[levelIndex] = scoreItem
                        }
                    }

                    withContext(Dispatchers.Main) { refreshScorePanel() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}