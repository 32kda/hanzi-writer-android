package com.hanziwriter.app.domain.algorithm

import com.hanziwriter.app.data.local.entity.CharacterProgress
import com.hanziwriter.app.domain.model.quiz.QuizCard

object CharacterSelector {

    fun selectForLearn(
        availableCards: List<QuizCard>,
        existingProgress: Map<Int, CharacterProgress>
    ): List<Int> {
        val newCards = availableCards.filter { card ->
            val progress = existingProgress[card.character.first().code]
            progress == null || progress.totalAttempts < 3
        }.take(2)
        return newCards.map { it.character.first().code }
    }

    fun selectForDrill(
        progressList: List<CharacterProgress>,
        count: Int = 5
    ): List<Int> {
        return progressList
            .filter { it.totalAttempts >= 3 }
            .sortedByDescending { PriorityCalculator.calculatePriority(it) }
            .take(count)
            .map { it.unicode }
    }

    fun selectForQuiz(
        progressList: List<CharacterProgress>,
        count: Int = 10
    ): List<Int> {
        val now = System.currentTimeMillis()
        val oneDayMs = 86_400_000L
        return progressList
            .filter { it.lastPracticed < now - oneDayMs }
            .sortedByDescending { PriorityCalculator.calculatePriority(it) }
            .take(count)
            .map { it.unicode }
    }
}
