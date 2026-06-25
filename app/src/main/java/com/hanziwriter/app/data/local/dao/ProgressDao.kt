package com.hanziwriter.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.hanziwriter.app.data.local.entity.CharacterProgress
import com.hanziwriter.app.data.local.entity.DailyEngagement
import com.hanziwriter.app.data.local.entity.StreakRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: CharacterProgress)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgressBatch(progressList: List<CharacterProgress>)

    @Query("SELECT * FROM character_progress WHERE unicode = :unicode")
    suspend fun getProgress(unicode: Int): CharacterProgress?

    @Query("SELECT * FROM character_progress WHERE activeSetName = :setName")
    suspend fun getAllProgressForSet(setName: String): List<CharacterProgress>

    @Query("SELECT * FROM character_progress WHERE activeSetName = :setName")
    fun observeAllProgressForSet(setName: String): Flow<List<CharacterProgress>>

    // Daily engagement

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyEngagement(engagement: DailyEngagement)

    @Query("SELECT * FROM daily_engagement WHERE date = :date")
    suspend fun getDailyEngagement(date: String): DailyEngagement?

    @Query("SELECT * FROM daily_engagement ORDER BY date DESC LIMIT 31")
    suspend fun getRecentEngagements(): List<DailyEngagement>

    @Query("SELECT COALESCE(SUM(totalTimeMinutes), 0) FROM daily_engagement WHERE date = :date")
    suspend fun getTotalMinutesForDate(date: String): Int

    // Streak

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStreak(streak: StreakRecord)

    @Query("SELECT * FROM streak WHERE id = 1")
    suspend fun getStreak(): StreakRecord?

    @Transaction
    suspend fun saveSessionResult(
        progressList: List<CharacterProgress>,
        engagement: DailyEngagement,
        streak: StreakRecord
    ) {
        upsertProgressBatch(progressList)
        upsertDailyEngagement(engagement)
        upsertStreak(streak)
    }
}
