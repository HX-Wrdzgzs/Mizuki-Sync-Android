package com.example.mizukisync.network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    // 1. 获取授权 (返回 AccessToken + RefreshToken)
    @GET("api/oauth/token")
    fun loginWithLX(@Query("code") code: String): Call<LoginResponse>

    // 2. 获取数据 (如果 Token 过期，后端会返回 "登录过期")
    @GET("api/user/profile")
    fun getProfile(): Call<LoginResponse>

    // 3. 令牌续期 (用旧的 RefreshToken 换取全新 30 天的 Token 套餐)
    @POST("api/oauth/refresh")
    fun refreshToken(@Query("refresh_token") refreshToken: String): Call<LoginResponse>

    // 4. 乐曲搜索 (补回这个接口，解决报错)
    @GET("api/music/search")
    fun searchMusic(@Query("keyword") keyword: String): Call<List<SongResult>>
}