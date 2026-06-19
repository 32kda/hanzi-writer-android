package com.hanziwriter.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hanziwriter.app.data.local.dao.CharacterDao
import com.hanziwriter.app.data.local.dao.ProgressDao
import com.hanziwriter.app.data.local.entity.CharacterEntity
import com.hanziwriter.app.data.local.entity.CharacterProgress
import com.hanziwriter.app.data.local.entity.DailyEngagement
import com.hanziwriter.app.data.local.entity.StreakRecord
import com.hanziwriter.app.data.local.entity.StrokeDataEntity

@Database(
    entities = [
        CharacterEntity::class,
        StrokeDataEntity::class,
        CharacterProgress::class,
        DailyEngagement::class,
        StreakRecord::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun progressDao(): ProgressDao
}
