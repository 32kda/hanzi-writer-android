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
        availableCards: List<QuizCard>,
        count: Int = 5
    ): List<Int> {
        val trainedUnicodes = progressList.map { it.unicode }.toSet()
        val result = linkedSetOf<Int>()

        // 1. Pick trained characters (≥3 attempts) in priority order
        result.addAll(progressList
            .filter { it.totalAttempts >= 3 }
            .sortedByDescending { PriorityCalculator.calculatePriority(it) }
            .take(count)
            .map { it.unicode }
        )

        // 2. If there aren't enough trained chars, fill with untrained ones
        if (result.size < count) {
            availableCards
                .map { it.character.first().code }
                .filter { it !in trainedUnicodes && it !in result }
                .take(count - result.size)
                .forEach { result.add(it) }
        }

        return result.toList()
    }

    fun selectForQuiz(
        progressList: List<CharacterProgress>,
        availableCards: List<QuizCard>,
        count: Int = 10
    ): List<Int> {
        val now = System.currentTimeMillis()
        val oneDayMs = 86_400_000L
        val trainedUnicodes = progressList.map { it.unicode }.toSet()
        val result = linkedSetOf<Int>()

        // 1. Pick trained characters due for review (>24h since last practice)
        result.addAll(progressList
            .filter { it.lastPracticed < now - oneDayMs }
            .sortedByDescending { PriorityCalculator.calculatePriority(it) }
            .take(count)
            .map { it.unicode }
        )

        // 2. If there aren't enough, fill with untrained characters
        if (result.size < count) {
            availableCards
                .map { it.character.first().code }
                .filter { it !in trainedUnicodes && it !in result }
                .take(count - result.size)
                .forEach { result.add(it) }
        }

        return result.toList()
    }
}
