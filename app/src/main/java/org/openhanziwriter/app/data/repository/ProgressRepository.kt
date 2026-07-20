package org.openhanziwriter.app.data.repository

import org.openhanziwriter.app.data.local.dao.ProgressDao
import org.openhanziwriter.app.data.local.entity.CharacterProgress
import org.openhanziwriter.app.data.local.entity.DailyEngagement
import org.openhanziwriter.app.data.local.entity.DaysPracticed
import org.openhanziwriter.app.data.local.entity.StreakRecord
import org.openhanziwriter.app.domain.algorithm.ProgressInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val _progressCache = MutableStateFlow<Map<Int, CharacterProgress>>(emptyMap())
    val progressCache: StateFlow<Map<Int, CharacterProgress>> = _progressCache.asStateFlow()

    suspend fun loadAllProgress() {
        val all = progressDao.getAllProgress()
        _progressCache.value = all.associateBy { it.unicode }
    }

    fun dropCache() {
        _progressCache.value = emptyMap()
    }

    fun getProgressInfo(unicodes: List<Int>): Map<Int, ProgressInfo> {
        return _progressCache.value.filterKeys { it in unicodes }
            .mapValues { (_, p) -> ProgressInfo(p.lastPracticed, p.timesPracticed) }
    }

    suspend fun endSession(
        characterStats: List<SessionCharacterStats>,
        activityType: String,
        sessionMinutes: Int,
        date: String,
        timestamp: Long
    ) {
        val cache = _progressCache.value.toMutableMap()

        val progressList = characterStats.map { stats ->
            val existing = cache[stats.unicode]
            val sessionAccuracy = if (stats.totalAttempts > 0) {
                stats.correctAttempts.toDouble() / stats.totalAttempts
            } else 0.0

            val updated = if (existing != null) {
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
                    timesPracticed = 1
                )
            }
            cache[stats.unicode] = updated
            updated
        }

        _progressCache.value = cache

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
                val lastDate = LocalDate.parse(existingStreak.lastActiveDate)
                val currentDate = LocalDate.parse(date)
                if (lastDate.plusDays(1) == currentDate) {
                    existingStreak.currentStreak + 1
                } else {
                    1
                }
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

        val todayEpochDay = LocalDate.now().toEpochDay().toInt()
        progressDao.saveSessionResult(progressList, updatedEngagement, streak, DaysPracticed(todayEpochDay))
    }

    suspend fun checkStreakOnStartup() {
        val existingStreak = progressDao.getStreak() ?: return
        val today = LocalDate.now().toString()
        if (existingStreak.lastActiveDate == today) return
        val lastDate = LocalDate.parse(existingStreak.lastActiveDate)
        val currentDate = LocalDate.parse(today)
        if (lastDate.plusDays(1) == currentDate) return
        val reset = existingStreak.copy(currentStreak = 0)
        progressDao.upsertStreak(reset)
    }

    suspend fun getProgress(unicode: Int): CharacterProgress? =
        progressDao.getProgress(unicode)

    suspend fun getStreak(): StreakRecord? = progressDao.getStreak()

    fun observeStreak(): Flow<StreakRecord?> = progressDao.observeStreak()

    suspend fun getTotalMinutesForDate(date: String): Int =
        progressDao.getTotalMinutesForDate(date)

    suspend fun getRecentEngagements(): List<DailyEngagement> =
        progressDao.getRecentEngagements()

    suspend fun getAllDaysPracticed(): List<Int> =
        progressDao.getAllDaysPracticed()
}
