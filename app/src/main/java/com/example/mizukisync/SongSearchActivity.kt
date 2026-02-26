package com.example.mizukisync

import android.content.Intent
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
import androidx.appcompat.app.AlertDialog
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

    // 懒加载网络客户端，防卡死
    private val client by lazy { UnsafeOkHttpClient.getClient() }
    private val songAdapter = SongAdapter()

    // 记录当前选择的筛选定数
    private var currentLevelFilter = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_search)

        rvSongList = findViewById(R.id.rv_song_list)
        etSearchKeyword = findViewById(R.id.et_search_keyword)

        rvSongList.layoutManager = LinearLayoutManager(this)
        rvSongList.adapter = songAdapter

        // 🌟 高级筛选菜单：弹出定数选择器！
        findViewById<ImageView>(R.id.btn_filter).setOnClickListener {
            val levels = arrayOf("全部显示", "13", "13+", "14", "14+", "15")
            AlertDialog.Builder(this)
                .setTitle("筛选谱面定数")
                .setItems(levels) { _, which ->
                    currentLevelFilter = if (which == 0) "" else levels[which]
                    val toastMsg = if (currentLevelFilter.isEmpty()) "已取消筛选" else "正在筛选: $currentLevelFilter"
                    Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()

                    // 带着筛选条件重新搜索
                    performSearch(etSearchKeyword.text.toString().trim())
                }.show()
        }

        etSearchKeyword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(etSearchKeyword.text.toString().trim())
                true
            } else false
        }

        // 默认加载
        performSearch("")
    }

    private fun performSearch(keyword: String) {
        Toast.makeText(this, "正在调取大后方数据...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 拼接带搜索词和筛选条件的 URL (先在前端做过滤)
                val url = if (keyword.isNotEmpty()) {
                    if (keyword.all { it.isDigit() }) {
                        "https://api.mizuki.top/api/songs/search?keyword=$keyword&id=$keyword"
                    } else {
                        "https://api.mizuki.top/api/songs/search?keyword=$keyword"
                    }
                } else {
                    "https://api.mizuki.top/api/songs/search?limit=100"
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
                                val song = dataArray.getJSONObject(i)

                                // 前端定数过滤
                                if (currentLevelFilter.isNotEmpty()) {
                                    var hasLevel = false
                                    val diffObj = song.optJSONObject("difficulties")
                                    val dx = diffObj?.optJSONArray("dx")
                                    val std = diffObj?.optJSONArray("standard")

                                    if (dx != null) {
                                        for (j in 0 until dx.length()) {
                                            if (dx.getJSONObject(j).optString("level") == currentLevelFilter) hasLevel = true
                                        }
                                    }
                                    if (std != null) {
                                        for (j in 0 until std.length()) {
                                            if (std.getJSONObject(j).optString("level") == currentLevelFilter) hasLevel = true
                                        }
                                    }
                                    if (!hasLevel) continue
                                }

                                songList.add(song)
                            }
                        }

                        withContext(Dispatchers.Main) {
                            songAdapter.updateData(songList)
                            if (songList.isEmpty()) {
                                Toast.makeText(this@SongSearchActivity, "没有找到符合条件的乐曲", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(this@SongSearchActivity, "网络异常: ${e.message}", Toast.LENGTH_SHORT).show() }
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
            if (diffArray == null || diffArray.length() == 0) diffArray = diffObj?.optJSONArray("standard")

            if (diffArray != null) {
                val colors = arrayOf("#4CAF50", "#FBC02D", "#F44336", "#9C27B0", "#E1BEE7")
                for (i in 0 until diffArray.length()) {
                    val level = diffArray.getJSONObject(i).optString("level", "?")
                    val chip = TextView(holder.itemView.context).apply {
                        text = level
                        textSize = 12f
                        setTextColor(if (i == 4) Color.parseColor("#6A1B9A") else Color.WHITE)
                        setPadding(16, 4, 16, 4)
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = 20f
                            setColor(Color.parseColor(if (i < colors.size) colors[i] else "#9E9E9E"))
                        }
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { setMargins(0, 0, 12, 0) }
                    }
                    holder.llDifficulties.addView(chip)
                }
            }

            // 🌟 点击卡片，跳跃到你绝美的猛男粉详情页！
            holder.itemView.setOnClickListener {
                val intent = Intent(this@SongSearchActivity, SongDetailActivity::class.java)
                intent.putExtra("song_data", song.toString())
                startActivity(intent)
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