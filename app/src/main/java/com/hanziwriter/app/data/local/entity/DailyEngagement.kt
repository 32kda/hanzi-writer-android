package com.hanziwriter.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_engagement")
data class DailyEngagement(
    @PrimaryKey val date: String,
    val totalTimeMinutes: Int,
    val engagementLevel: String,
    val activitiesCompleted: String,
    val charactersLearned: Int,
    val charactersDrilled: Int,
    val charactersQuizzed: Int,
    val quizScore: Int? = null
)
