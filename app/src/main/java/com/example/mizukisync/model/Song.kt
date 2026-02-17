package com.example.mizukisync.model

import com.google.gson.annotations.SerializedName

/**
 * 对应后端 music_data.json 中的单条歌曲数据
 */
data class Song(
    @SerializedName("song_id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("artist") val artist: String,
    @SerializedName("type") val type: String, // DX 或 Standard
    @SerializedName("level") val levels: List<String> // 比如 ["7", "9", "12", "13+"]
)

// 后端返回的列表包装类
data class SongListResponse(
    val songs: List<Song>
)