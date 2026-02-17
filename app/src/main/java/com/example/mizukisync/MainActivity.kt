package com.example.mizukisync

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.mizukisync.network.*
import retrofit2.*

class MainActivity : AppCompatActivity() {

    private lateinit var ivAvatar: ImageView
    private lateinit var ivBgPlate: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var tvRating: TextView
    private lateinit var tvFriendCode: TextView
    private lateinit var tvTrophy: TextView
    private lateinit var tvDan: TextView
    private lateinit var tvClass: TextView
    private lateinit var tvStar: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 绑定 ID
        ivAvatar = findViewById(R.id.iv_avatar)
        ivBgPlate = findViewById(R.id.iv_bg_plate)
        tvUsername = findViewById(R.id.tv_username)
        tvRating = findViewById(R.id.tv_rating)
        tvFriendCode = findViewById(R.id.tv_friend_code)
        tvTrophy = findViewById(R.id.tv_trophy)
        tvDan = findViewById(R.id.tv_dan)
        tvClass = findViewById(R.id.tv_class)
        tvStar = findViewById(R.id.tv_star)

        // 功能卡片
        setupCard(R.id.btn_song_search, "🎵", "乐曲搜索", "查询全曲库") {
            startActivity(Intent(this, SongSearchActivity::class.java))
        }
        setupCard(R.id.btn_refresh, "🔄", "刷新API", "同步最新数据") { loadData() }
        setupCard(R.id.btn_logout_grid, "🚪", "退出登录", "清除缓存") {
            getSharedPreferences("mizuki_prefs", Context.MODE_PRIVATE).edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // 启动自动加载
        loadData()
    }

    private fun setupCard(viewId: Int, icon: String, title: String, sub: String, onClick: () -> Unit) {
        val card = findViewById<View>(viewId)
        if (card != null) {
            card.findViewById<TextView>(R.id.tv_icon).text = icon
            card.findViewById<TextView>(R.id.tv_title).text = title
            card.findViewById<TextView>(R.id.tv_subtitle).text = sub
            card.setOnClickListener { onClick() }
        }
    }

    private fun loadData() {
        RetrofitClient.getInstance(this).getProfile().enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    tvUsername.text = user.username
                    tvRating.text = user.maimai_rating.toString()
                    tvFriendCode.text = "ID: ${user.friend_code}"
                    tvTrophy.text = user.trophy ?: "暂无称号"
                    tvDan.text = user.dan ?: "初学者"
                    tvClass.text = user.class_rank ?: "B1"
                    tvStar.text = "★×${user.star ?: 0}"

                    if (!user.icon_url.isNullOrEmpty()) {
                        Glide.with(this@MainActivity).load(user.icon_url).transform(RoundedCorners(20)).into(ivAvatar)
                    }
                    if (!user.plate_url.isNullOrEmpty()) {
                        Glide.with(this@MainActivity).load(user.plate_url).centerCrop().into(ivBgPlate)
                    }
                }
            }
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}