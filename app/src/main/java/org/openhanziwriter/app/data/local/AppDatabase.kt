package org.openhanziwriter.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import org.openhanziwriter.app.data.local.dao.ProgressDao
import org.openhanziwriter.app.data.local.entity.CharacterProgress
import org.openhanziwriter.app.data.local.entity.DailyEngagement
import org.openhanziwriter.app.data.local.entity.DaysPracticed
import org.openhanziwriter.app.data.local.entity.StreakRecord

@Database(
    entities = [
        CharacterProgress::class,
        DailyEngagement::class,
        DaysPracticed::class,
        StreakRecord::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao
}
