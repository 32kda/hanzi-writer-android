package com.hanziwriter.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "character_progress")
data class CharacterProgress(
    @PrimaryKey val unicode: Int,
    val accuracy: Double,
    val lastPracticed: Long,
    val timesPracticed: Int,
    val activeSetName: String
)
