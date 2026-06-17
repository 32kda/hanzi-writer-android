package com.hanziwriter.app.data.repository

import com.hanziwriter.app.data.local.dao.ProgressDao
import com.hanziwriter.app.data.local.entity.CharacterProgress
import com.hanziwriter.app.data.local.entity.DailyEngagement
import com.hanziwriter.app.data.local.entity.StreakRecord
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepository @Inject constructor(
    private val progressDao: ProgressDao
) {
    suspend fun saveStrokeAttempt(unicode: Int, result: Boolean, timestamp: Long) {
        val existing = progressDao.getProgress(unicode)
        val updated = if (existing != null) {
            existing.copy(
                totalAttempts = existing.totalAttempts + 1,
                correctAttempts = existing.correctAttempts + (if (result) 1 else 0),
                consecutiveCorrect = if (result) existing.consecutiveCorrect + 1 else 0,
                lastPracticed = timestamp,
                lastResult = if (result) "CORRECT" else "WRONG",
                accuracy = (existing.correctAttempts + (if (result) 1 else 0)).toDouble() /
                        (existing.totalAttempts + 1)
            )
        } else {
            CharacterProgress(
                unicode = unicode,
                accuracy = if (result) 1.0 else 0.0,
                totalAttempts = 1,
                correctAttempts = if (result) 1 else 0,
                consecutiveCorrect = if (result) 1 else 0,
                lastPracticed = timestamp,
                lastResult = if (result) "CORRECT" else "WRONG",
                averageResponseTimeMs = 0,
                hintUsageCount = 0,
                introducedDate = timestamp,
                isLearned = false,
                activeSetName = "hsk1_en"
            )
        }
        progressDao.upsertProgress(updated)
    }

    suspend fun getProgress(unicode: Int): CharacterProgress? =
        progressDao.getProgress(unicode)

    suspend fun getAllProgressForSet(setName: String): List<CharacterProgress> =
        progressDao.getAllProgressForSet(setName)

    fun observeAllProgressForSet(setName: String): Flow<List<CharacterProgress>> =
        progressDao.observeAllProgressForSet(setName)

    suspend fun getLearnedCount(setName: String): Int =
        progressDao.getLearnedCount(setName)

    suspend fun getTotalPracticedCount(setName: String): Int =
        progressDao.getTotalPracticedCount(setName)

    // Daily engagement

    suspend fun addActivity(date: String, type: String, minutes: Int) {
        val existing = progressDao.getDailyEngagement(date)
        if (existing != null) {
            val updated = existing.copy(
                totalTimeMinutes = existing.totalTimeMinutes + minutes,
                activitiesCompleted = if (existing.activitiesCompleted.contains(type))
                    existing.activitiesCompleted else "$type,${existing.activitiesCompleted}"
            )
            progressDao.upsertDailyEngagement(updated)
        } else {
            progressDao.upsertDailyEngagement(
                DailyEngagement(
                    date = date,
                    totalTimeMinutes = minutes,
                    engagementLevel = if (minutes >= 20) "STRONG" else if (minutes >= 10) "MODERATE" else "LIGHT",
                    activitiesCompleted = type,
                    charactersLearned = 0,
                    charactersDrilled = 0,
                    charactersQuizzed = 0
                )
            )
        }
    }

    suspend fun getTotalMinutesForDate(date: String): Int =
        progressDao.getTotalMinutesForDate(date)

    suspend fun getRecentEngagements(): List<DailyEngagement> =
        progressDao.getRecentEngagements()

    // Streak

    suspend fun getStreak(): StreakRecord? = progressDao.getStreak()

    suspend fun updateStreak(date: String) {
        val existing = progressDao.getStreak()
        val updated = if (existing != null) {
            val newStreak = if (existing.lastActiveDate == date) {
                existing.currentStreak
            } else {
                existing.currentStreak + 1
            }
            existing.copy(
                currentStreak = newStreak,
                longestStreak = maxOf(existing.longestStreak, newStreak),
                lastActiveDate = date
            )
        } else {
            StreakRecord(
                id = 1,
                currentStreak = 1,
                longestStreak = 1,
                lastActiveDate = date
            )
        }
        progressDao.upsertStreak(updated)
    }
}
