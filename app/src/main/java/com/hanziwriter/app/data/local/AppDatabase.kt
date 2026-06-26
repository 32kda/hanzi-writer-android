package com.hanziwriter.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun progressDao(): ProgressDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE character_progress_new (
                        unicode INTEGER NOT NULL,
                        accuracy REAL NOT NULL,
                        lastPracticed INTEGER NOT NULL,
                        timesPracticed INTEGER NOT NULL,
                        activeSetName TEXT NOT NULL,
                        PRIMARY KEY(unicode),
                        FOREIGN KEY(unicode) REFERENCES characters(unicode) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO character_progress_new (unicode, accuracy, lastPracticed, timesPracticed, activeSetName)
                    SELECT unicode, accuracy, lastPracticed, 0, activeSetName
                    FROM character_progress
                """.trimIndent())
                db.execSQL("DROP TABLE character_progress")
                db.execSQL("ALTER TABLE character_progress_new RENAME TO character_progress")

                db.execSQL("""
                    CREATE TABLE daily_engagement_new (
                        date TEXT NOT NULL,
                        totalTimeMinutes INTEGER NOT NULL,
                        engagementLevel TEXT NOT NULL,
                        activitiesCompleted TEXT NOT NULL,
                        charactersLearned INTEGER NOT NULL,
                        charactersDrilled INTEGER NOT NULL,
                        charactersQuizzed INTEGER NOT NULL,
                        PRIMARY KEY(date)
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO daily_engagement_new (
                        date, totalTimeMinutes, engagementLevel,
                        activitiesCompleted, charactersLearned,
                        charactersDrilled, charactersQuizzed
                    )
                    SELECT
                        date, totalTimeMinutes, engagementLevel,
                        activitiesCompleted, charactersLearned,
                        charactersDrilled, charactersQuizzed
                    FROM daily_engagement
                """.trimIndent())
                db.execSQL("DROP TABLE daily_engagement")
                db.execSQL("ALTER TABLE daily_engagement_new RENAME TO daily_engagement")
            }
        }
    }
}
