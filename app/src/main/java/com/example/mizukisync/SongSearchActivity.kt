package com.example.mizukisync

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.mizukisync.network.UnsafeOkHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

class SongSearchActivity : AppCompatActivity() {

    private lateinit var rvSongList: RecyclerView
    private lateinit var etSearchKeyword: EditText

    // 懒加载无敌网络客户端，防卡死
    private val client by lazy { UnsafeOkHttpClient.getClient() }

    private val songAdapter = SongAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_search)

        rvSongList = findViewById(R.id.rv_song_list)
        etSearchKeyword = findViewById(R.id.et_search_keyword)

        rvSongList.layoutManager = LinearLayoutManager(this)
        rvSongList.adapter = songAdapter

        // 🌟 给右上角的筛选漏斗（三条杠）注入灵魂
        findViewById<ImageView>(R.id.btn_filter).setOnClickListener {
            Toast.makeText(this, "高级筛选功能（定数/版本）火热开发中！", Toast.LENGTH_SHORT).show()
        }

        // 监听搜索键盘动作
        etSearchKeyword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val keyword = etSearchKeyword.text.toString().trim()
                performSearch(keyword)
                true
            } else {
                false
            }
        }

        // 进页面先拉前 50 首歌验货
        performSearch("")
    }

    private fun performSearch(keyword: String) {
        Toast.makeText(this, "正在从大后方调取数据...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 🌟 核心升级：增加纯数字 ID 识别逻辑
                val url = if (keyword.isNotEmpty()) {
                    if (keyword.all { it.isDigit() }) {
                        // 如果用户输入的全是数字，通知后端按 ID 查询！
                        "https://api.mizuki.top/api/songs/search?keyword=$keyword&id=$keyword"
                    } else {
                        "https://api.mizuki.top/api/songs/search?keyword=$keyword"
                    }
                } else {
                    "https://api.mizuki.top/api/songs/search"
                }

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val jsonString = response.body()?.string() ?: ""

                if (response.isSuccessful && jsonString.isNotEmpty()) {
                    val jsonObject = JSONObject(jsonString)

                    if (jsonObject.optBoolean("success", false)) {
                        val dataArray = jsonObject.optJSONArray("data")
                        val songList = mutableListOf<JSONObject>()

                        if (dataArray != null) {
                            for (i in 0 until dataArray.length()) {
                                songList.add(dataArray.getJSONObject(i))
                            }
                        }

                        // 切回主线程刷新界面
                        withContext(Dispatchers.Main) {
                            songAdapter.updateData(songList)
                            if (songList.isEmpty()) {
                                Toast.makeText(this@SongSearchActivity, "没有找到相关乐曲，请检查输入", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SongSearchActivity, "请求失败或暂无数据", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SongSearchActivity, "网络异常: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    inner class SongAdapter : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {
        private var songs = listOf<JSONObject>()

        fun updateData(newSongs: List<JSONObject>) {
            songs = newSongs
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_song_card, parent, false)
            return SongViewHolder(view)
        }

        override fun getItemCount(): Int = songs.size

        override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
            val song = songs[position]

            holder.tvTitle.text = song.optString("title", "未知曲目")
            val artist = song.optString("artist", "未知曲师")
            val bpm = song.optInt("bpm", 0)
            holder.tvArtistBpm.text = "$artist / BPM: $bpm"

            val jacketUrl = song.optString("jacket_url", "")
            Glide.with(holder.itemView.context)
                .load(jacketUrl)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(16)))
                .into(holder.ivJacket)

            holder.llDifficulties.removeAllViews()

            val diffObj = song.optJSONObject("difficulties")
            var diffArray = diffObj?.optJSONArray("dx")
            if (diffArray == null || diffArray.length() == 0) {
                diffArray = diffObj?.optJSONArray("standard")
            }

            if (diffArray != null) {
                val colors = arrayOf("#4CAF50", "#FBC02D", "#F44336", "#9C27B0", "#E0E0E0")
                for (i in 0 until diffArray.length()) {
                    val diffData = diffArray.getJSONObject(i)
                    val level = diffData.optString("level", "?")

                    val chip = TextView(holder.itemView.context).apply {
                        text = level
                        textSize = 12f
                        setTextColor(if (i == 4) Color.parseColor("#424242") else Color.WHITE)
                        setPadding(16, 4, 16, 4)

                        val shape = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = 20f
                            setColor(Color.parseColor(if (i < colors.size) colors[i] else "#9E9E9E"))
                        }
                        background = shape

                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { setMargins(0, 0, 12, 0) }
                    }
                    holder.llDifficulties.addView(chip)
                }
            }
        }

        inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ivJacket: ImageView = itemView.findViewById(R.id.iv_song_jacket)
            val tvTitle: TextView = itemView.findViewById(R.id.tv_song_title)
            val tvArtistBpm: TextView = itemView.findViewById(R.id.tv_song_artist_bpm)
            val llDifficulties: LinearLayout = itemView.findViewById(R.id.ll_difficulties)
        }
    }
}