package com.hanziwriter.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streak")
data class StreakRecord(
    @PrimaryKey val id: Int = 1,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastActiveDate: String
)
