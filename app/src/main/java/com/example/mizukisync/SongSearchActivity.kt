package com.example.mizukisync

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mizukisync.network.UnsafeOkHttpClient
import kotlinx.coroutines.*
import okhttp3.Request
import org.json.JSONObject
import java.io.File

class SongSearchActivity : AppCompatActivity() {

    private lateinit var rvSongs: RecyclerView
    private lateinit var etSearch: EditText
    private val client by lazy { UnsafeOkHttpClient.getUnsafeOkHttpClient() }
    private val songAdapter = SongAdapter()
    private var allSongsList = mutableListOf<JSONObject>()
    private val cacheFile by lazy { File(filesDir, "song_database.json") }

    // 筛选状态存储
    private var filterMinConst = 0.0
    private var filterMaxConst = 20.0
    private var filterVersion: String? = null
    private var filterCategory: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_search)

        rvSongs = findViewById(R.id.rv_songs)
        etSearch = findViewById(R.id.et_search)
        rvSongs.layoutManager = LinearLayoutManager(this)
        rvSongs.adapter = songAdapter

        // 🌟 修复：绑定三个筛选按钮的点击事件
        findViewById<View>(R.id.btn_filter_level).setOnClickListener { showLevelFilterDialog() }
        findViewById<View>(R.id.btn_filter_version).setOnClickListener { showListFilterDialog("版本", "version") }
        findViewById<View>(R.id.btn_filter_type).setOnClickListener { showListFilterDialog("类别", "genre") }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyAllFilters() }
            override fun beforeTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(s: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })

        initSongDatabase()
    }

    private fun initSongDatabase() {
        CoroutineScope(Dispatchers.IO).launch {
            if (cacheFile.exists()) parseAndShow(cacheFile.readText())
            try {
                // 向 api.mizuki.top 获取最新全量曲库
                val url = "https://api.mizuki.top/api/songs/search?limit=9999"
                val resp = client.newCall(Request.Builder().url(url).build()).execute()
                val body = resp.body?.string() ?: ""
                if (resp.isSuccessful && body.isNotEmpty()) {
                    cacheFile.writeText(body)
                    parseAndShow(body)
                }
            } catch (e: Exception) { Log.e("MizukiSync", "Update failed", e) }
        }
    }

    private suspend fun parseAndShow(json: String) {
        try {
            val root = JSONObject(json)
            val dataArray = root.optJSONArray("data") ?: return
            val tempList = mutableListOf<JSONObject>()
            for (i in 0 until dataArray.length()) tempList.add(dataArray.getJSONObject(i))
            withContext(Dispatchers.Main) {
                allSongsList = tempList
                applyAllFilters()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // 🌟 定数筛选弹窗
    private fun showLevelFilterDialog() {
        val v = LayoutInflater.from(this).inflate(R.layout.dialog_filter_level, null)
        val dialog = AlertDialog.Builder(this).setView(v).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val etMin = v.findViewById<EditText>(R.id.et_min_const)
        val etMax = v.findViewById<EditText>(R.id.et_max_const)
        if (filterMinConst > 0.0) etMin.setText(filterMinConst.toString())
        if (filterMaxConst < 20.0) etMax.setText(filterMaxConst.toString())

        v.findViewById<View>(R.id.btn_filter_reset).setOnClickListener {
            filterMinConst = 0.0; filterMaxConst = 20.0; applyAllFilters(); dialog.dismiss()
        }
        v.findViewById<View>(R.id.btn_filter_confirm).setOnClickListener {
            filterMinConst = etMin.text.toString().toDoubleOrNull() ?: 0.0
            filterMaxConst = etMax.text.toString().toDoubleOrNull() ?: 20.0
            applyAllFilters(); dialog.dismiss()
        }
        dialog.show()
    }

    // 🌟 修复：版本/类别筛选弹窗
    private fun showListFilterDialog(title: String, field: String) {
        val items = allSongsList.map { it.optString(field) }.distinct().filter { it.isNotEmpty() }.sorted().toMutableList()
        items.add(0, "全部$title")

        val listView = ListView(this).apply {
            adapter = ArrayAdapter(this@SongSearchActivity, android.R.layout.simple_list_item_1, items)
            divider = ColorDrawable(Color.parseColor("#E2E8F0"))
            dividerHeight = 1
        }

        // 🌟 修复：修正 radius 赋值和 dpToPx 调用
        val container = CardView(this).apply {
            radius = 24.dpToPx().toFloat() // 核心修复：Int 上调用 dpToPx
            setCardBackgroundColor(Color.WHITE)
            cardElevation = 0f

            val inner = LinearLayout(this@SongSearchActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(24.dpToPx(), 24.dpToPx(), 24.dpToPx(), 24.dpToPx())
                addView(TextView(this@SongSearchActivity).apply {
                    text = "选择$title"
                    textSize = 18f
                    setTextColor(Color.parseColor("#1E293B"))
                    typeface = Typeface.DEFAULT_BOLD
                })
                val lp = LinearLayout.LayoutParams(-1, 800)
                lp.topMargin = 16.dpToPx()
                addView(listView, lp)
            }
            addView(inner)
        }

        val dialog = AlertDialog.Builder(this).setView(container).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        listView.setOnItemClickListener { _, _, position, _ ->
            if (position == 0) {
                if (field == "version") filterVersion = null else filterCategory = null
            } else {
                val selected = items[position]
                if (field == "version") filterVersion = selected else filterCategory = selected
            }
            applyAllFilters()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun applyAllFilters() {
        val keyword = etSearch.text.toString().lowercase()
        val filtered = allSongsList.filter { s ->
            val kMatch = keyword.isEmpty() || s.optString("title").lowercase().contains(keyword) || s.optString("id").contains(keyword)
            val vMatch = filterVersion == null || s.optString("version") == filterVersion
            val cMatch = filterCategory == null || s.optString("genre") == filterCategory
            val dList = s.optJSONObject("difficulties")?.optJSONArray("standard")
            var dMatch = false
            if (dList != null) {
                for (i in 0 until dList.length()) {
                    val ds = dList.getJSONObject(i).optDouble("level_value", 0.0)
                    if (ds in filterMinConst..filterMaxConst) { dMatch = true; break }
                }
            }
            kMatch && vMatch && cMatch && (filterMinConst == 0.0 && filterMaxConst == 20.0 || dMatch)
        }
        songAdapter.updateList(filtered)
    }

    // 🌟 核心：工具函数
    private fun Int.dpToPx() = (this * resources.displayMetrics.density).toInt()

    inner class SongAdapter : RecyclerView.Adapter<SongAdapter.VH>() {
        private var data = listOf<JSONObject>()
        fun updateList(newList: List<JSONObject>) { data = newList; notifyDataSetChanged() }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_song_card, p, false))
        override fun getItemCount() = data.size
        override fun onBindViewHolder(h: VH, p: Int) {
            val s = data[p]
            val songId = s.optInt("id")
            h.title.text = s.optString("title")

            // 🌟 修复：正确显示艺术家和 BPM
            val artist = s.optString("artist", "Unknown")
            val bpm = s.optString("bpm", "---")
            h.info.text = "$artist / BPM: $bpm"

            val jacketUrl = "https://assets2.lxns.net/maimai/jacket/${songId}.png"
            Glide.with(h.itemView.context).load(jacketUrl).placeholder(android.R.drawable.ic_menu_report_image).into(h.iv)

            h.itemView.setOnClickListener {
                val intent = Intent(this@SongSearchActivity, SongDetailActivity::class.java)
                intent.putExtra("song_id", songId)
                startActivity(intent)
            }
        }

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val iv: ImageView = v.findViewById(R.id.iv_song_jacket)
            val title: TextView = v.findViewById(R.id.tv_song_title)
            val info: TextView = v.findViewById(R.id.tv_song_artist) // 对应 item_song_card.xml
        }
    }
}