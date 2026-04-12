package com.example.mizukisync

import kotlin.math.floor

object MaimaiAlgorithm {

    /**
     * 计算单曲 Rating
     */
    fun calculateRating(levelValue: Double, achievement: Double): Int {
        val factor = getAchievementFactor(achievement)
        // 计算公式：定数 * 达成率系数 * 因子 (向下取整)
        return floor(levelValue * (achievement.coerceAtMost(100.5) / 100.0) * factor).toInt()
    }

    private fun getAchievementFactor(achievement: Double): Double {
        return when {
            achievement >= 100.5000 -> 22.4
            achievement >= 100.0000 -> 21.6
            achievement >= 99.5000 -> 21.1
            achievement >= 99.0000 -> 20.8
            achievement >= 98.0000 -> 20.3
            achievement >= 97.0000 -> 20.0
            achievement >= 94.0000 -> 16.8
            achievement >= 90.0000 -> 15.2
            achievement >= 80.0000 -> 13.6
            achievement >= 75.0000 -> 12.0
            achievement >= 70.0000 -> 11.2
            achievement >= 60.0000 -> 9.6
            achievement >= 50.0000 -> 8.0
            else -> 0.0
        }
    }
}