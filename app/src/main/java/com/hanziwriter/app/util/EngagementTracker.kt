package com.hanziwriter.app.util

import com.hanziwriter.app.data.repository.ProgressRepository
import java.time.LocalDate

class EngagementTracker(
    private val progressRepository: ProgressRepository
) {
    private var sessionStartTime: Long = 0L
    private var accumulatedTimeMs: Long = 0L

    fun startSession() {
        sessionStartTime = System.currentTimeMillis()
    }

    fun pauseSession() {
        accumulatedTimeMs += System.currentTimeMillis() - sessionStartTime
        sessionStartTime = 0L
    }

    fun resumeSession() {
        sessionStartTime = System.currentTimeMillis()
    }

    suspend fun endSession(activityType: String) {
        if (sessionStartTime > 0L) {
            accumulatedTimeMs += System.currentTimeMillis() - sessionStartTime
        }

        val minutes = (accumulatedTimeMs / 60_000).toInt().coerceAtLeast(1)
        val today = LocalDate.now().toString()

        progressRepository.addActivity(today, activityType, minutes)
        progressRepository.updateStreak(today)
    }
}
