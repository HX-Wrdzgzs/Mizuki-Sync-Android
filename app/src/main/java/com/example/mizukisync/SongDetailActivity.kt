package com.example.mizukisync

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import org.json.JSONObject

class SongDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        // 🌟 终极防线：捕捉极其罕见的 XML 崩溃
        try {
            setContentView(R.layout.activity_song_detail)
        } catch (e: Throwable) {
            Toast.makeText(this, "严重错误：布局文件渲染失败，请检查 XML", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        findViewById<View>(R.id.btn_back_circle)?.setOnClickListener { finish() }

        val songJsonStr = intent.getStringExtra("song_data") ?: ""
        if (songJsonStr.isEmpty()) {
            Toast.makeText(this, "数据传输失败", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        try {
            val song = JSONObject(songJsonStr)
            setupUI(song)
        } catch (e: Throwable) {
            Toast.makeText(this, "数据解析异常", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupUI(song: JSONObject) {
        findViewById<TextView>(R.id.tv_detail_title)?.text = song.optString("title", "未知曲目")
        findViewById<TextView>(R.id.tv_detail_artist)?.text = song.optString("artist", "未知曲师")
        findViewById<TextView>(R.id.tv_detail_bpm)?.text = song.optInt("bpm", 0).toString()
        findViewById<TextView>(R.id.tv_detail_id)?.text = "#${song.optString("id", "???")}"

        val basicInfo = song.optJSONObject("basic_info")
        val version = basicInfo?.optString("version", song.optString("version", "未知版本")) ?: "未知版本"
        val category = basicInfo?.optString("genre", song.optString("genre", "未知类别")) ?: "未知类别"

        findViewById<TextView>(R.id.tv_detail_version)?.text = version
        findViewById<TextView>(R.id.tv_detail_category)?.text = category

        val jacketUrl = song.optString("jacket_url", "")
        findViewById<ImageView>(R.id.iv_header_bg)?.let { Glide.with(this).load(jacketUrl).into(it) }
        findViewById<ImageView>(R.id.iv_detail_jacket_pop)?.let { Glide.with(this).load(jacketUrl).into(it) }

        val diffObj = song.optJSONObject("difficulties")
        var diffArray = diffObj?.optJSONArray("dx")
        if (diffArray == null || diffArray.length() == 0) diffArray = diffObj?.optJSONArray("standard")

        if (diffArray != null && diffArray.length() > 0) {
            val targetDiff = diffArray.getJSONObject(diffArray.length() - 1)

            val levelValue = targetDiff.optString("level_value", targetDiff.optString("level", "?"))
            findViewById<TextView>(R.id.tv_detail_const)?.text = levelValue
            findViewById<TextView>(R.id.tv_detail_designer)?.text = targetDiff.optString("note_designer", "未知谱师")

            val notesObj = targetDiff.optJSONObject("notes")
            if (notesObj != null) {
                findViewById<TextView>(R.id.tv_detail_tap)?.text = notesObj.optInt("tap", 0).toString()
                findViewById<TextView>(R.id.tv_detail_hold)?.text = notesObj.optInt("hold", 0).toString()
                findViewById<TextView>(R.id.tv_detail_slide)?.text = notesObj.optInt("slide", 0).toString()
                findViewById<TextView>(R.id.tv_detail_touch)?.text = notesObj.optInt("touch", 0).toString()
                findViewById<TextView>(R.id.tv_detail_break)?.text = notesObj.optInt("break", 0).toString()
            }
        }
    }
}