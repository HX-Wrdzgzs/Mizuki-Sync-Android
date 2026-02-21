package com.example.mizukisync.network

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("token")
    val token: String,

    @SerializedName("refresh_token")
    val refresh_token: String = "",

    @SerializedName("username")
    val username: String,

    @SerializedName("maimai_rating")
    val maimai_rating: Int,

    @SerializedName("friend_code")
    val friend_code: String,

    @SerializedName("icon_url")
    val icon_url: String? = "",

    @SerializedName("plate_url")
    val plate_url: String? = "",

    @SerializedName("frame_url")
    val frame_url: String? = "",

    @SerializedName("trophy")
    val trophy: String? = "",

    @SerializedName("dan")
    val dan: String? = "",

    @SerializedName("class_rank")
    val class_rank: String? = "",

    @SerializedName("star")
    val star: Int = 0,

    @SerializedName("login_type")
    val login_type: String = "OAuth"
)