package com.hanziwriter.app.domain.algorithm

import kotlin.random.Random

data class ProgressInfo(
    val lastPracticed: Long,
    val timesPracticed: Int
)

object CharacterSelector {

    private const val MS_PER_DAY: Double = 86_400_000.0
    private const val DECAY_DAYS: Double = 14.0

    fun select(
        unicodes: List<Int>,
        progress: Map<Int, ProgressInfo>,
        count: Int
    ): List<Int> {
        if (unicodes.isEmpty() || count <= 0) return emptyList()
        val n = count.coerceAtMost(unicodes.size)

        val now = System.currentTimeMillis()

        // Step 1-2: compute score for each character, build histogram
        val scores = IntArray(unicodes.size)
        val histogram = mutableMapOf<Int, Int>()

        for (i in unicodes.indices) {
            val info = progress[unicodes[i]]
            val score = if (info != null) {
                val daysSince = (now - info.lastPracticed) / MS_PER_DAY
                maxOf(0, info.timesPracticed - (daysSince / DECAY_DAYS).toInt())
            } else {
                0
            }
            scores[i] = score
            histogram[score] = (histogram[score] ?: 0) + 1
        }

        // Step 3-4: sort scores, mark categories
        val sortedScores = histogram.keys.sorted()
        var pending = n

        val addAllScores = mutableSetOf<Int>()
        var addRandomScore: Int? = null

        for (score in sortedScores) {
            val scoreCount = histogram[score] ?: continue
            if (scoreCount <= pending) {
                addAllScores.add(score)
                pending -= scoreCount
            } else {
                addRandomScore = score
                break
            }
        }

        // Step 5: build result
        val result = mutableListOf<Int>()

        // Add all characters for "add all" scores
        for (i in unicodes.indices) {
            if (scores[i] in addAllScores) {
                result.add(unicodes[i])
            }
        }

        // Reservoir sampling for "add random" score
        if (addRandomScore != null && pending > 0) {
            val candidates = mutableListOf<Int>()
            for (i in unicodes.indices) {
                if (scores[i] == addRandomScore) {
                    candidates.add(i)
                }
            }
            // Shuffle and take first `pending`
            candidates.shuffle(Random)
            for (i in 0 until pending.coerceAtMost(candidates.size)) {
                result.add(unicodes[candidates[i]])
            }
        }

        return result
    }
}
