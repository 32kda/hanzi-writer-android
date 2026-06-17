package com.hanziwriter.app.domain.model.quiz

import com.hanziwriter.app.domain.model.character.Character
import com.hanziwriter.app.domain.model.geometry.Point
import com.hanziwriter.app.domain.model.state.RenderState

class Quiz(
    val character: Character,
    private val renderState: RenderState
) {
    var currentStrokeIndex = 0
        private set
    var mistakesOnStroke = 0
        private set
    var totalMistakes = 0
        private set
    var isActive = false
        private set

    private var currentUserStroke: UserStroke? = null
    private var strokeIdCounter = 0
    var options: QuizOptions? = null
        private set

    data class UserStroke(
        val id: Int,
        val points: MutableList<Point> = mutableListOf()
    ) {
        constructor(id: Int, startPoint: Point) : this(id, mutableListOf(startPoint))
    }

    data class UserStrokeResult(
        val strokeId: Int,
        val started: Boolean
    )

    data class QuizResult(
        var character: String = "",
        var strokeNum: Int = 0,
        var mistakesOnStroke: Int = 0,
        var totalMistakes: Int = 0,
        var strokesRemaining: Int = 0,
        var isCorrect: Boolean = false,
        var isBackwards: Boolean = false
    )

    data class QuizOptions(
        val leniency: Double = 1.0,
        val strokeFadeDuration: Double = 1000.0,
        val drawingFadeDuration: Double = 300.0,
        val acceptBackwardsStrokes: Boolean = false,
        val showHintAfterMisses: Int? = 3,
        val onCorrectStroke: ((QuizResult) -> Unit)? = null,
        val onMistake: ((QuizResult) -> Unit)? = null,
        val onComplete: ((QuizResult) -> Unit)? = null
    )

    fun start(options: QuizOptions) {
        this.options = options
        this.isActive = true
        this.currentStrokeIndex = 0
        this.mistakesOnStroke = 0
        this.totalMistakes = 0

        renderState.setMainOpacity(1.0)
        renderState.setHighlightOpacity(1.0)
        for (i in 0 until character.strokeCount) {
            renderState.setStrokeOpacity("main", i.toString(), 0.25)
            renderState.setStrokeDisplayPortion("main", i.toString(), 1.0)
            renderState.setStrokeOpacity("highlight", i.toString(), 0.0)
            renderState.setStrokeDisplayPortion("highlight", i.toString(), 1.0)
        }
    }

    fun cancel() {
        isActive = false
        currentUserStroke = null
    }

    fun startUserStroke(externalPoint: Point): UserStrokeResult? {
        if (!isActive) return null
        if (currentUserStroke != null) {
            endUserStroke()
        }
        val strokeId = ++strokeIdCounter
        currentUserStroke = UserStroke(strokeId, externalPoint)
        return UserStrokeResult(strokeId, true)
    }

    fun continueUserStroke(externalPoint: Point) {
        currentUserStroke?.points?.add(externalPoint)
    }

    fun endUserStroke(callback: ((QuizResult) -> Unit)? = null) {
        val stroke = currentUserStroke ?: return
        if (stroke.points.size == 1) {
            currentUserStroke = null
            return
        }

        val matchResult = StrokeMatcher.checkMatch(
            stroke.points,
            character,
            currentStrokeIndex,
            options?.leniency ?: 1.0
        )

        val acceptBackwards = options?.acceptBackwardsStrokes ?: false
        val isAccepted = matchResult.isMatch ||
                (matchResult.isStrokeBackwards && acceptBackwards)

        if (!isAccepted) {
            if (mistakesOnStroke == 0) totalMistakes++
            mistakesOnStroke++
        }

        val result = QuizResult().apply {
            character = this@Quiz.character.symbol
            strokeNum = currentStrokeIndex
            this.mistakesOnStroke = this@Quiz.mistakesOnStroke
            this.totalMistakes = this@Quiz.totalMistakes
            strokesRemaining = this@Quiz.character.strokeCount - currentStrokeIndex - (if (isAccepted) 1 else 0)
            this.isCorrect = isAccepted
            isBackwards = matchResult.isStrokeBackwards
        }

        if (isAccepted) {
            renderState.setStrokeOpacity("main", currentStrokeIndex.toString(), 1.0)
            renderState.setStrokeDisplayPortion("main", currentStrokeIndex.toString(), 1.0)

            currentStrokeIndex++
            mistakesOnStroke = 0

            if (currentStrokeIndex >= character.strokeCount) {
                isActive = false
                options?.onComplete?.invoke(result)
            }

            options?.onCorrectStroke?.invoke(result)
        } else {
            options?.onMistake?.invoke(result)
            val hintAfter = options?.showHintAfterMisses
            if (hintAfter != null && mistakesOnStroke >= hintAfter) {
                renderState.setStrokeOpacity("highlight", currentStrokeIndex.toString(), 1.0)
            }
        }

        currentUserStroke = null
        callback?.invoke(result)
    }
}
