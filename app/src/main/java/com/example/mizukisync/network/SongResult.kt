package com.example.mizukisync.network

import com.google.gson.annotations.SerializedName

data class SongResult(
    @SerializedName("id")
    val id: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("artist")
    val artist: String? = "",

    @SerializedName("cover_url")
    val cover_url: String,

    @SerializedName("level")
    val level: String,

    @SerializedName("user_score")
    val user_score: String,

    @SerializedName("rate_icon")
    val rate_icon: String,

    @SerializedName("achievements")
    val achievements: Float,

    @SerializedName("source")
    val source: String = "DF"
)