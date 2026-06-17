package com.hanziwriter.app.domain.model.quiz

import com.hanziwriter.app.domain.model.character.Character
import com.hanziwriter.app.domain.model.geometry.GeometryUtils
import com.hanziwriter.app.domain.model.geometry.Point

object StrokeMatcher {

    private const val COSINE_SIMILARITY_THRESHOLD = 0.0
    private const val START_AND_END_DIST_THRESHOLD = 250.0
    private const val FRECHET_THRESHOLD = 0.4
    private const val MIN_LEN_THRESHOLD = 0.35
    private const val AVERAGE_DISTANCE_THRESHOLD = 350.0

    data class MatchResult(
        val isMatch: Boolean,
        val averageDistance: Double,
        val isStrokeBackwards: Boolean
    )

    fun checkMatch(
        userPoints: List<Point>,
        character: Character,
        strokeNum: Int,
        leniency: Double = 1.0
    ): MatchResult {
        return checkMatchImpl(userPoints, character, strokeNum, leniency, true)
    }

    private fun checkMatchImpl(
        userPoints: List<Point>,
        character: Character,
        strokeNum: Int,
        leniency: Double,
        checkBackwards: Boolean
    ): MatchResult {
        val points = GeometryUtils.stripDuplicates(userPoints)
        if (points.size < 2) {
            return MatchResult(false, 0.0, false)
        }

        val targetStroke = character.strokes[strokeNum]
        val avgDist = targetStroke.getAverageDistance(points)

        val distMod = 0.5
        val withinDistThresh = avgDist <= AVERAGE_DISTANCE_THRESHOLD * distMod * leniency

        if (!withinDistThresh) {
            return MatchResult(false, avgDist, false)
        }

        val startAndEndMatch = startAndEndMatches(points, targetStroke, leniency)
        val directionMatch = directionMatches(points, targetStroke)
        val shapeMatch = shapeMatches(points, targetStroke, leniency)
        val lengthMatch = lengthMatches(points, targetStroke, leniency)

        val isMatch = startAndEndMatch && directionMatch && shapeMatch && lengthMatch

        if (checkBackwards && !isMatch) {
            val reversed = points.reversed()
            val backwardsResult = checkMatchImpl(reversed, character, strokeNum, leniency, false)
            if (backwardsResult.isMatch) {
                return MatchResult(false, avgDist, true)
            }
        }

        return MatchResult(isMatch, avgDist, false)
    }

    private fun startAndEndMatches(
        points: List<Point>,
        stroke: com.hanziwriter.app.domain.model.character.Stroke,
        leniency: Double
    ): Boolean {
        val startPoint = points.first()
        val endPoint = points.last()
        val startingDist = GeometryUtils.distance(startPoint, stroke.startingPoint)
        val endingDist = GeometryUtils.distance(endPoint, stroke.endingPoint)
        return startingDist <= START_AND_END_DIST_THRESHOLD * leniency
                && endingDist <= START_AND_END_DIST_THRESHOLD * leniency
    }

    private fun directionMatches(
        points: List<Point>,
        stroke: com.hanziwriter.app.domain.model.character.Stroke
    ): Boolean {
        val edgeVectors = GeometryUtils.getEdgeVectors(points)
        val strokeVectors = stroke.vectors
        if (edgeVectors.isEmpty()) return true
        val totalSimilarity = edgeVectors.sumOf { edgeVector ->
            strokeVectors.maxOf { strokeVector ->
                GeometryUtils.cosineSimilarity(strokeVector, edgeVector)
            }
        }
        return totalSimilarity / edgeVectors.size > COSINE_SIMILARITY_THRESHOLD
    }

    private fun shapeMatches(
        points: List<Point>,
        stroke: com.hanziwriter.app.domain.model.character.Stroke,
        leniency: Double
    ): Boolean = GeometryUtils.shapeFit(points, stroke.points, leniency)

    private fun lengthMatches(
        points: List<Point>,
        stroke: com.hanziwriter.app.domain.model.character.Stroke,
        leniency: Double
    ): Boolean {
        val userLength = GeometryUtils.length(points)
        val strokeLength = stroke.length
        if (strokeLength == 0.0) return true
        return leniency * (userLength + 25) / (strokeLength + 25) >= MIN_LEN_THRESHOLD
    }
}
