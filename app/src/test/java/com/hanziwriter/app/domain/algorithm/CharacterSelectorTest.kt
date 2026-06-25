package com.hanziwriter.app.domain.algorithm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterSelectorTest {

    private fun progressInfo(timesPracticed: Int, lastPracticed: Long): ProgressInfo =
        ProgressInfo(lastPracticed, timesPracticed)

    @Test
    fun `select returns empty for empty unicodes`() {
        val result = CharacterSelector.select(emptyList(), emptyMap(), count = 5)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `select returns empty for zero count`() {
        val unicodes = listOf(1, 2, 3)
        val result = CharacterSelector.select(unicodes, emptyMap(), count = 0)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `select returns all when count exceeds size`() {
        val unicodes = listOf(1, 2, 3)
        val result = CharacterSelector.select(unicodes, emptyMap(), count = 10)
        assertEquals(3, result.size)
        assertTrue(result.containsAll(unicodes))
    }

    @Test
    fun `select picks from lowest score first`() {
        val now = System.currentTimeMillis()
        val oneDayMs = 86_400_000L

        val unicodes = listOf(1, 2, 3, 4, 5)
        val progress = mapOf(
            1 to progressInfo(timesPracticed = 5, lastPracticed = now),                    // recently practiced, score ~5
            2 to progressInfo(timesPracticed = 0, lastPracticed = now - 30 * oneDayMs),     // days/14 ≈ 2.14, score = 0
            3 to progressInfo(timesPracticed = 1, lastPracticed = now - 7 * oneDayMs),      // days/14 = 0.5, score = 0.5
            4 to progressInfo(timesPracticed = 3, lastPracticed = now - 14 * oneDayMs),     // days/14 = 1, score = 2
            5 to progressInfo(timesPracticed = 10, lastPracticed = now - 140 * oneDayMs)    // days/14 = 10, score = 0
        )
        // Scores: 1→~5, 2→0, 3→0.5, 4→2, 5→0
        // Sorted: 0 (chars 2,5), 0.5 (char 3), 2 (char 4), 5 (char 1)
        // Selecting 2 should give chars 2 and 5

        val result = CharacterSelector.select(unicodes, progress, count = 2)
        assertEquals(2, result.size)
        assertTrue(result.containsAll(listOf(2, 5)))
    }

    @Test
    fun `select new characters get score zero`() {
        val now = System.currentTimeMillis()

        val unicodes = listOf(1, 2, 3)
        val progress = mapOf(
            1 to progressInfo(timesPracticed = 10, lastPracticed = now) // score ~10, practiced recently
        )
        // Scores: 1→~10, 2→0 (new), 3→0 (new)
        // Selecting 2 should give chars 2 and 3

        val result = CharacterSelector.select(unicodes, progress, count = 2)
        assertEquals(2, result.size)
        assertEquals(setOf(2, 3), result.toSet())
    }

    @Test
    fun `select respects histogram boundary from spec example`() {
        // Example from spec: N=10, score 0→3, score 1→2, score 3→20, score 4→15
        // Result: all of 0(3) + all of 1(2) = 5, then 5 random from score 3
        val now = System.currentTimeMillis()
        val oneDayMs = 86_400_000L

        // score 0: timesPracticed=0, old lastPracticed → days/14 large, score=0
        val score0 = listOf(1, 2, 3).associateWith {
            progressInfo(timesPracticed = 0, lastPracticed = now - 100 * oneDayMs)
        }
        // score 1: timesPracticed=1, lastPracticed=now → score=1
        val score1 = listOf(4, 5).associateWith {
            progressInfo(timesPracticed = 1, lastPracticed = now)
        }
        // score 3: timesPracticed=3, lastPracticed=now → score=3
        val score3 = (10..29).associateWith {
            progressInfo(timesPracticed = 3, lastPracticed = now)
        }
        // score 4: timesPracticed=4, lastPracticed=now → score=4
        val score4 = (30..44).associateWith {
            progressInfo(timesPracticed = 4, lastPracticed = now)
        }

        val progress = score0 + score1 + score3 + score4
        val unicodes = progress.keys.toList()

        val result = CharacterSelector.select(unicodes, progress, count = 10)
        assertEquals(10, result.size)
        // Should contain all of score 0 and score 1 characters
        assertTrue(result.containsAll(listOf(1, 2, 3, 4, 5)))
        // Remaining 5 should come from score 3 group
        val fromScore3 = result.filter { it in 10..29 }
        assertEquals(5, fromScore3.size)
        // Should not contain any from score 4
        val fromScore4 = result.filter { it in 30..44 }
        assertTrue(fromScore4.isEmpty())
    }

    @Test
    fun `select deterministic when all scores are same`() {
        val unicodes = (1..20).toList()
        val result = CharacterSelector.select(unicodes, emptyMap(), count = 10)
        assertEquals(10, result.size)
        assertTrue(result.all { it in unicodes })
    }

    @Test
    fun `select count equals input size when smaller than count`() {
        val unicodes = listOf(1, 2, 3)
        val result = CharacterSelector.select(unicodes, emptyMap(), count = 5)
        assertEquals(3, result.size)
    }

    @Test
    fun `select score calculation with days since practice`() {
        val now = System.currentTimeMillis()
        val oneDayMs = 86_400_000L

        // Character practiced once, 14 days ago → daysSince/14 = 1, score = max(0, 1-1) = 0
        val unicodes = listOf(42)
        val progress = mapOf(
            42 to progressInfo(timesPracticed = 1, lastPracticed = now - 14 * oneDayMs)
        )
        val result = CharacterSelector.select(unicodes, progress, count = 1)
        assertEquals(listOf(42), result)
    }
}
