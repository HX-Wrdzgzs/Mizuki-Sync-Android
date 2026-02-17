package com.example.mizukisync

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

        ivAvatar = findViewById(R.id.iv_avatar)
        ivBgPlate = findViewById(R.id.iv_bg_plate)
        tvUsername = findViewById(R.id.tv_username)
        tvRating = findViewById(R.id.tv_rating)
        tvFriendCode = findViewById(R.id.tv_friend_code)
        tvTrophy = findViewById(R.id.tv_trophy)
        tvDan = findViewById(R.id.tv_dan)
        tvClass = findViewById(R.id.tv_class)
        tvStar = findViewById(R.id.tv_star)

        // 按钮
        setupCard(R.id.btn_song_search, "🎵", "乐曲搜索", "查询全曲库") { startActivity(Intent(this, SongSearchActivity::class.java)) }
        setupCard(R.id.btn_score_query, "📊", "成绩查询", "查看 B35/B15") { Toast.makeText(this, "开发中", Toast.LENGTH_SHORT).show() }
        setupCard(R.id.btn_bind_df, "🌊", "绑定水鱼", "同步开发者 Token") { Toast.makeText(this, "暂未启用", Toast.LENGTH_SHORT).show() }
        setupCard(R.id.btn_tools, "🧰", "工具箱", "常用小工具") { Toast.makeText(this, "开发中", Toast.LENGTH_SHORT).show() }
        setupCard(R.id.btn_refresh, "🔄", "刷新数据", "同步最新记录") { loadData() }
        setupCard(R.id.btn_logout_grid, "🚪", "退出登录", "清除缓存") {
            getSharedPreferences("mizuki_prefs", MODE_PRIVATE).edit().clear().apply()
            finish()
        }

        loadData()
    }

    private fun setupCard(viewId: Int, icon: String, title: String, subtitle: String, onClick: () -> Unit) {
        val card = findViewById<View>(viewId)
        if (card != null) {
            card.findViewById<TextView>(R.id.tv_icon)?.text = icon
            card.findViewById<TextView>(R.id.tv_title)?.text = title
            card.findViewById<TextView>(R.id.tv_subtitle)?.text = subtitle
            card.setOnClickListener { onClick() }
        }
    }

    private fun loadData() {
        RetrofitClient.getInstance(this).getProfile().enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!

                    // 直接显示，不通过逻辑修改
                    tvUsername.text = user.username
                    tvRating.text = user.maimai_rating.toString()
                    tvFriendCode.text = "ID: ${user.friend_code}"
                    tvTrophy.text = user.trophy ?: ""
                    tvDan.text = user.dan ?: ""
                    tvClass.text = user.class_rank ?: ""
                    tvStar.text = "★×${user.star ?: 0}"

                    if (!user.icon_url.isNullOrEmpty()) {
                        Glide.with(this@MainActivity).load(user.icon_url).transform(RoundedCorners(20)).into(ivAvatar)
                    }
                    if (!user.plate_url.isNullOrEmpty()) {
                        Glide.with(this@MainActivity).load(user.plate_url).centerCrop().into(ivBgPlate)
                    }
                } else {
                    tvUsername.text = "同步失败"
                    Toast.makeText(this@MainActivity, "错误码: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                tvUsername.text = "网络错误"
                Toast.makeText(this@MainActivity, t.message, Toast.LENGTH_LONG).show()
            }
        })
    }
}