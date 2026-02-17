package com.example.mizukisync

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.mizukisync.network.*
import retrofit2.*

class SongSearchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_search)

        val etSearch = findViewById<EditText>(R.id.et_search)
        val btnSearch = findViewById<Button>(R.id.btn_do_search)
        val rvSongs = findViewById<RecyclerView>(R.id.rv_songs)

        rvSongs.layoutManager = LinearLayoutManager(this)

        btnSearch.setOnClickListener {
            val keyword = etSearch.text.toString().trim()
            // 只要有字就搜，什么都不要管
            if (keyword.isNotEmpty()) {
                search(keyword, rvSongs)
            }
        }
    }

    private fun search(keyword: String, rv: RecyclerView) {
        Toast.makeText(this, "搜索中...", Toast.LENGTH_SHORT).show()
        RetrofitClient.getInstance(this).searchMusic(keyword).enqueue(object : Callback<List<SongResult>> {
            override fun onResponse(call: Call<List<SongResult>>, response: Response<List<SongResult>>) {
                if (response.isSuccessful && response.body() != null) {
                    val list = response.body()!!
                    if (list.isEmpty()) Toast.makeText(this@SongSearchActivity, "无结果", Toast.LENGTH_SHORT).show()
                    rv.adapter = SongAdapter(list)
                } else {
                    Toast.makeText(this@SongSearchActivity, "失败: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<List<SongResult>>, t: Throwable) {
                Toast.makeText(this@SongSearchActivity, "网络错误", Toast.LENGTH_SHORT).show()
            }
        })
    }

    inner class SongAdapter(private val songs: List<SongResult>) : RecyclerView.Adapter<SongAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val title: TextView = v.findViewById(R.id.tv_title)
            val artist: TextView = v.findViewById(R.id.tv_artist)
            val ids: TextView = v.findViewById(R.id.tv_ids)
            val score: TextView = v.findViewById(R.id.tv_score)
            val rate: TextView = v.findViewById(R.id.tv_rate)
            val level: TextView = v.findViewById(R.id.tv_level)
            val cover: ImageView = v.findViewById(R.id.iv_cover)
            val root: View = v
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val s = songs[position]
            holder.title.text = s.title
            holder.artist.text = s.artist ?: ""
            holder.ids.text = "ID: ${s.id}"
            holder.score.text = s.user_score
            holder.rate.text = s.rate_icon
            holder.level.text = s.level
            Glide.with(this@SongSearchActivity).load(s.cover_url).transform(RoundedCorners(12)).into(holder.cover)
        }

        override fun getItemCount() = songs.size
    }
}