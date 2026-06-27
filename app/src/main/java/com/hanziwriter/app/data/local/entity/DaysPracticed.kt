package com.hanziwriter.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "days_practiced")
data class DaysPracticed(
    @PrimaryKey val day: Int
)
