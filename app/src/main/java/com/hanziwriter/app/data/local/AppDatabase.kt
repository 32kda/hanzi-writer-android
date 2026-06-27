package com.hanziwriter.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hanziwriter.app.data.local.dao.ProgressDao
import com.hanziwriter.app.data.local.entity.CharacterProgress
import com.hanziwriter.app.data.local.entity.DailyEngagement
import com.hanziwriter.app.data.local.entity.DaysPracticed
import com.hanziwriter.app.data.local.entity.StreakRecord

@Database(
    entities = [
        CharacterProgress::class,
        DailyEngagement::class,
        DaysPracticed::class,
        StreakRecord::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao
}
