package com.hanziwriter.app.data.repository

import com.hanziwriter.app.data.local.dao.ProgressDao
import com.hanziwriter.app.data.local.entity.CharacterProgress
import com.hanziwriter.app.data.local.entity.DailyEngagement
import com.hanziwriter.app.data.local.entity.DaysPracticed
import com.hanziwriter.app.data.local.entity.StreakRecord
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class SessionCharacterStats(
    val unicode: Int,
    val totalAttempts: Int,
    val correctAttempts: Int
)

@Singleton
class ProgressRepository @Inject constructor(
    private val progressDao: ProgressDao
) {
    suspend fun endSession(
        setName: String,
        characterStats: List<SessionCharacterStats>,
        activityType: String,
        sessionMinutes: Int,
        date: String,
        timestamp: Long
    ) {
        val progressList = characterStats.map { stats ->
            val existing = progressDao.getProgress(stats.unicode)
            val sessionAccuracy = if (stats.totalAttempts > 0) {
                stats.correctAttempts.toDouble() / stats.totalAttempts
            } else 0.0

            if (existing != null) {
                val newTimesPracticed = existing.timesPracticed + 1
                val newAccuracy = (existing.accuracy * existing.timesPracticed + sessionAccuracy) / newTimesPracticed
                existing.copy(
                    accuracy = newAccuracy,
                    lastPracticed = timestamp,
                    timesPracticed = newTimesPracticed
                )
            } else {
                CharacterProgress(
                    unicode = stats.unicode,
                    accuracy = sessionAccuracy,
                    lastPracticed = timestamp,
                    timesPracticed = 1,
                    activeSetName = setName
                )
            }
        }

        val engagement = progressDao.getDailyEngagement(date)
        val updatedEngagement = if (engagement != null) {
            val newMinutes = engagement.totalTimeMinutes + sessionMinutes
            engagement.copy(
                totalTimeMinutes = newMinutes,
                engagementLevel = if (newMinutes >= 20) "STRONG" else if (newMinutes >= 10) "MODERATE" else "LIGHT",
                activitiesCompleted = if (engagement.activitiesCompleted.contains(activityType))
                    engagement.activitiesCompleted else engagement.activitiesCompleted + ",$activityType",
                charactersLearned = engagement.charactersLearned + if (activityType == "learn") characterStats.size else 0,
                charactersDrilled = engagement.charactersDrilled + if (activityType == "drill") characterStats.size else 0,
                charactersQuizzed = engagement.charactersQuizzed + if (activityType == "quiz") characterStats.size else 0
            )
        } else {
            DailyEngagement(
                date = date,
                totalTimeMinutes = sessionMinutes,
                engagementLevel = if (sessionMinutes >= 20) "STRONG" else if (sessionMinutes >= 10) "MODERATE" else "LIGHT",
                activitiesCompleted = activityType,
                charactersLearned = if (activityType == "learn") characterStats.size else 0,
                charactersDrilled = if (activityType == "drill") characterStats.size else 0,
                charactersQuizzed = if (activityType == "quiz") characterStats.size else 0
            )
        }

        val existingStreak = progressDao.getStreak()
        val streak = if (existingStreak != null) {
            val newStreak = if (existingStreak.lastActiveDate == date) {
                existingStreak.currentStreak
            } else {
                existingStreak.currentStreak + 1
            }
            existingStreak.copy(
                currentStreak = newStreak,
                longestStreak = maxOf(existingStreak.longestStreak, newStreak),
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

        progressDao.saveSessionResult(progressList, updatedEngagement, streak)

        val todayEpochDay = LocalDate.now().toEpochDay().toInt()
        progressDao.insertDaysPracticed(DaysPracticed(todayEpochDay))
    }

    suspend fun getProgress(unicode: Int): CharacterProgress? =
        progressDao.getProgress(unicode)

    suspend fun getAllProgressForSet(setName: String): List<CharacterProgress> =
        progressDao.getAllProgressForSet(setName)

    fun observeAllProgressForSet(setName: String): Flow<List<CharacterProgress>> =
        progressDao.observeAllProgressForSet(setName)

    suspend fun getStreak(): StreakRecord? = progressDao.getStreak()

    fun observeStreak(): Flow<StreakRecord?> = progressDao.observeStreak()

    suspend fun getTotalMinutesForDate(date: String): Int =
        progressDao.getTotalMinutesForDate(date)

    suspend fun getRecentEngagements(): List<DailyEngagement> =
        progressDao.getRecentEngagements()

    suspend fun getAllDaysPracticed(): List<Int> =
        progressDao.getAllDaysPracticed()
}
