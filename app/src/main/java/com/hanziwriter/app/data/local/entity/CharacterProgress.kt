package com.hanziwriter.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "character_progress",
    foreignKeys = [ForeignKey(
        entity = CharacterEntity::class,
        parentColumns = ["unicode"],
        childColumns = ["unicode"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class CharacterProgress(
    @PrimaryKey val unicode: Int,
    val accuracy: Double,
    val totalAttempts: Int,
    val correctAttempts: Int,
    val consecutiveCorrect: Int,
    val lastPracticed: Long,
    val lastResult: String,
    val averageResponseTimeMs: Long,
    val hintUsageCount: Int,
    val introducedDate: Long,
    val isLearned: Boolean,
    val activeSetName: String
)
