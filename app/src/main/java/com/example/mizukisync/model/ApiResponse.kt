package com.example.mizukisync.model

import com.google.gson.annotations.SerializedName

/**
 * 这是全项目唯一的 ServerStatus 模型
 */
data class ServerStatus(
    @SerializedName("status")
    val status: String = "unknown",

    @SerializedName("message")
    val message: String = "",

    @SerializedName("endpoints")
    val endpoints: Map<String, String> = emptyMap()
)