package com.hanziwriter.app.domain.algorithm

import com.hanziwriter.app.data.local.entity.CharacterProgress
import kotlin.math.exp
import kotlin.math.pow

object PriorityCalculator {

    private const val MIN_EXPOSURES = 3

    fun calculatePriority(character: CharacterProgress): Double {
        val baseDifficulty = 1.0 / (character.accuracy.coerceAtLeast(0.01)).pow(2)
        val daysSinceReview = daysSince(character.lastPracticed)
        val decayFactor = exp(-daysSinceReview / halfLife(character.accuracy))
        val exposurePenalty = if (character.totalAttempts < MIN_EXPOSURES) 2.0 else 1.0
        val streakMultiplier = 1.0 + (character.consecutiveCorrect / 10.0)
        val recencyBoost = if (character.lastResult == "WRONG") 1.3 else 1.0

        return baseDifficulty * decayFactor * exposurePenalty * streakMultiplier * recencyBoost
    }

    private fun halfLife(accuracy: Double): Double = when {
        accuracy < 0.50 -> 1.0
        accuracy < 0.80 -> 3.0
        else -> 7.0
    }

    private fun daysSince(timestampMs: Long): Double {
        if (timestampMs == 0L) return 999.0
        val msInDay = 86_400_000.0
        return (System.currentTimeMillis() - timestampMs) / msInDay
    }
}
