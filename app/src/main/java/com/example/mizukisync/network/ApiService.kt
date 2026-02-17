package com.example.mizukisync.network

import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @GET("/")
    fun getServerStatus(): Call<Void>

    // 登录接口 (必须有这两个，LoginActivity 才能用)
    @GET("/api/oauth/token")
    fun loginWithLX(@Query("code") code: String): Call<LoginResponse>

    @GET("/api/df/login")
    fun loginWithDF(@Query("token") token: String): Call<LoginResponse>

    // 获取资料
    @GET("/api/user/profile")
    fun getProfile(): Call<LoginResponse>

    // 搜索
    @GET("/api/music/search")
    fun searchMusic(@Query("keyword") keyword: String): Call<List<SongResult>>
}

// --- 数据类必须在 interface 外面！！！ ---
data class LoginResponse(
    val token: String,
    val username: String,
    val maimai_rating: Int,
    val rating_lx: Int,
    val rating_df: Int,
    val friend_code: String,
    val login_type: String,
    val icon_url: String?,
    val plate_url: String?,
    val trophy: String?,
    val dan: String?,
    val class_rank: String?,
    val star: Int?
)

data class SongResult(
    val id: Int,
    val title: String,
    val artist: String?,
    val cover_url: String,
    val level: String,
    val user_score: String,
    val rate_icon: String,
    val achievements: Double,
    val source: String
)