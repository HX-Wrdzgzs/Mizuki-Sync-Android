package com.example.mizukisync

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import org.json.JSONObject

class SongDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 隐藏自带标题栏，让巨大的背景图顶穿屏幕边缘
        supportActionBar?.hide()
        setContentView(R.layout.activity_song_detail)

        // 🌟 绑定左上角返回按钮，点击瞬间销毁当前页面
        findViewById<ImageView>(R.id.btn_back)?.setOnClickListener {
            finish()
        }

        val songJsonStr = intent.getStringExtra("song_data") ?: ""
        if (songJsonStr.isEmpty()) {
            Toast.makeText(this, "数据加载失败", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        try {
            val song = JSONObject(songJsonStr)
            setupUI(song)
        } catch (e: Exception) {
            Toast.makeText(this, "数据解析异常", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupUI(song: JSONObject) {
        // 绑定你这套 UI 专属的 ID
        val ivHeaderBg = findViewById<ImageView>(R.id.iv_header_bg)
        val ivJacketPop = findViewById<ImageView>(R.id.iv_detail_jacket_pop)

        val tvTitle = findViewById<TextView>(R.id.tv_detail_title)
        val tvArtist = findViewById<TextView>(R.id.tv_detail_artist)
        val tvBpm = findViewById<TextView>(R.id.tv_detail_bpm)
        val tvId = findViewById<TextView>(R.id.tv_detail_id)

        // 填充数据
        tvTitle.text = song.optString("title", "未知曲目")
        tvArtist.text = song.optString("artist", "未知曲师")
        tvBpm.text = "BPM: ${song.optInt("bpm", 0)}"
        tvId.text = "ID: ${song.optString("id", "???")}"

        val jacketUrl = song.optString("jacket_url", "")

        // 加载底层巨大的氛围背景图
        Glide.with(this)
            .load(jacketUrl)
            .into(ivHeaderBg)

        // 加载中层悬浮的清晰曲绘
        Glide.with(this)
            .load(jacketUrl)
            .into(ivJacketPop)
    }
}