package com.hanziwriter.app.domain.algorithm

import com.hanziwriter.app.data.local.entity.CharacterProgress
import com.hanziwriter.app.domain.model.quiz.QuizCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterSelectorTest {

    private val cards = listOf(
        QuizCard("的", "de", "possessive"),
        QuizCard("一", "yī", "one"),
        QuizCard("是", "shì", "yes"),
        QuizCard("不", "bù", "no"),
        QuizCard("了", "le", "particle")
    )

    private fun progress(
        unicode: Int,
        totalAttempts: Int = 0,
        accuracy: Double = 0.0,
        lastPracticed: Long = 0L,
        lastResult: String = "NONE"
    ) = CharacterProgress(
        unicode = unicode,
        accuracy = accuracy,
        totalAttempts = totalAttempts,
        correctAttempts = if (totalAttempts > 0) (totalAttempts * accuracy).toInt() else 0,
        consecutiveCorrect = 0,
        lastPracticed = lastPracticed,
        lastResult = lastResult,
        averageResponseTimeMs = 0L,
        hintUsageCount = 0,
        introducedDate = 0L,
        isLearned = false,
        activeSetName = ""
    )

    @Test
    fun `selectForLearn returns up to 2 new characters when no progress exists`() {
        val result = CharacterSelector.selectForLearn(cards, emptyMap())
        assertEquals(2, result.size)
        assertEquals('的'.code, result[0])
        assertEquals('一'.code, result[1])
    }

    @Test
    fun `selectForLearn skips characters with 3 or more attempts`() {
        val progress = mapOf(
            '的'.code to progress(unicode = '的'.code, totalAttempts = 3),
            '一'.code to progress(unicode = '一'.code, totalAttempts = 2)
        )
        val result = CharacterSelector.selectForLearn(cards, progress)
        assertEquals(2, result.size)
        assertEquals('一'.code, result[0])
        assertEquals('是'.code, result[1])
    }

    @Test
    fun `selectForLearn returns fewer than 2 when insufficient new cards`() {
        val progress = mapOf(
            '的'.code to progress(unicode = '的'.code, totalAttempts = 5),
            '一'.code to progress(unicode = '一'.code, totalAttempts = 4),
            '是'.code to progress(unicode = '是'.code, totalAttempts = 3),
            '不'.code to progress(unicode = '不'.code, totalAttempts = 3),
            '了'.code to progress(unicode = '了'.code, totalAttempts = 3)
        )
        val result = CharacterSelector.selectForLearn(cards, progress)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `selectForLearn returns only unicode values from available cards`() {
        val result = CharacterSelector.selectForLearn(cards, emptyMap())
        result.forEach { unicode ->
            assertTrue(cards.any { it.character.first().code == unicode })
        }
    }

    @Test
    fun `selectForDrill returns characters sorted by priority`() {
        val now = System.currentTimeMillis()
        val progressList = listOf(
            progress(
                unicode = '的'.code, totalAttempts = 5, accuracy = 0.2,
                lastPracticed = now, lastResult = "WRONG"
            ),
            progress(
                unicode = '一'.code, totalAttempts = 5, accuracy = 0.9,
                lastPracticed = now, lastResult = "CORRECT"
            )
        )
        val result = CharacterSelector.selectForDrill(progressList, count = 2)
        assertEquals(2, result.size)
        assertEquals('的'.code, result[0])
        assertEquals('一'.code, result[1])
    }

    @Test
    fun `selectForDrill respects count parameter`() {
        val progressList = listOf(
            progress(unicode = '的'.code, totalAttempts = 5),
            progress(unicode = '一'.code, totalAttempts = 5),
            progress(unicode = '是'.code, totalAttempts = 5)
        )
        val result = CharacterSelector.selectForDrill(progressList, count = 2)
        assertEquals(2, result.size)
    }

    @Test
    fun `selectForDrill excludes characters with fewer than 3 attempts`() {
        val progressList = listOf(
            progress(unicode = '的'.code, totalAttempts = 2),
            progress(unicode = '一'.code, totalAttempts = 5, accuracy = 0.8)
        )
        val result = CharacterSelector.selectForDrill(progressList, count = 5)
        assertEquals(1, result.size)
        assertEquals('一'.code, result[0])
    }

    @Test
    fun `selectForQuiz filters by lastPracticed older than 24h`() {
        val now = System.currentTimeMillis()
        val oneDayMs = 86_400_000L
        val progressList = listOf(
            progress(unicode = '的'.code, totalAttempts = 5, accuracy = 0.5, lastPracticed = now - 2 * oneDayMs),
            progress(unicode = '一'.code, totalAttempts = 5, accuracy = 0.5, lastPracticed = now)
        )
        val result = CharacterSelector.selectForQuiz(progressList, count = 5)
        assertEquals(1, result.size)
        assertEquals('的'.code, result[0])
    }

    @Test
    fun `selectForQuiz respects count parameter`() {
        val now = System.currentTimeMillis()
        val oneDayMs = 86_400_000L
        val progressList = listOf(
            progress(unicode = '的'.code, totalAttempts = 3, accuracy = 0.5, lastPracticed = now - 2 * oneDayMs, lastResult = "WRONG"),
            progress(unicode = '一'.code, totalAttempts = 3, accuracy = 0.9, lastPracticed = now - 2 * oneDayMs, lastResult = "CORRECT")
        )
        val result = CharacterSelector.selectForQuiz(progressList, count = 1)
        assertEquals(1, result.size)
    }

    @Test
    fun `selectForQuiz returns empty when all characters practiced less than 24h ago`() {
        val now = System.currentTimeMillis()
        val progressList = listOf(
            progress(unicode = '的'.code, totalAttempts = 3, lastPracticed = now)
        )
        val result = CharacterSelector.selectForQuiz(progressList, count = 10)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `selectForDrill returns empty when no character has 3+ attempts`() {
        val progressList = listOf(
            progress(unicode = '的'.code, totalAttempts = 2)
        )
        val result = CharacterSelector.selectForDrill(progressList, count = 5)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `selectForLearn returns only new characters when mixed progress exists`() {
        val progress = mapOf(
            '的'.code to progress(unicode = '的'.code, totalAttempts = 3)
        )
        val result = CharacterSelector.selectForLearn(cards, progress)
        assertEquals(2, result.size)
        assertEquals('一'.code, result[0])
        assertEquals('是'.code, result[1])
    }
}
