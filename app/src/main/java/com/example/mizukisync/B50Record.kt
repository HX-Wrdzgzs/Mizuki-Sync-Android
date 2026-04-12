package com.example.mizukisync

data class B50Record(
    val id: Int,
    val title: String,
    val levelValue: Double,
    val achievement: Double,
    val rating: Int,
    val difficulty: Int,
    val jacketUrl: String,
    val versionType: String, // 🌟 核心修复：区分 standard 和 dx
    val genre: String,
    val version: String
)