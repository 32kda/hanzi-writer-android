package com.hanziwriter.app.domain.model.state

import com.hanziwriter.app.domain.model.character.Character
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RenderState(
    character: Character,
    options: Options = Options()
) {
    data class StrokeState(
        val opacity: Double,
        val displayPortion: Double
    )

    data class Options(
        val drawingColor: Color = Color(161, 197, 255),
        val strokeColor: Color = Color(34, 34, 34),
        val outlineColor: Color = Color(194, 194, 194),
        val radicalColor: Color = Color(34, 34, 34),
        val highlightColor: Color = Color(255, 0, 0, 0.5f),
        val showCharacter: Boolean = true,
        val showOutline: Boolean = true,
        val drawingWidth: Double = 20.0,
        val drawingFadeDuration: Double = 300.0
    )

    private val _mainStrokes = mutableMapOf<String, StrokeState>()
    private val _outlineStrokes = mutableMapOf<String, StrokeState>()
    private val _highlightStrokes = mutableMapOf<String, StrokeState>()

    private val _stateFlow = MutableStateFlow(this)
    val stateFlow: StateFlow<RenderState> = _stateFlow.asStateFlow()

    val drawingColor: Color = options.drawingColor
    val strokeColor: Color = options.strokeColor
    val outlineColor: Color = options.outlineColor
    val radicalColor: Color = options.radicalColor
    val highlightColor: Color = options.highlightColor

    var mainOpacity = 1.0
        private set
    var outlineOpacity = 0.0
        private set
    var highlightOpacity = 1.0
        private set

    var showCharacter = options.showCharacter
        private set
    var showOutline = options.showOutline
        private set
    val drawingWidth: Double = options.drawingWidth
    val drawingFadeDuration: Double = options.drawingFadeDuration

    val mainStrokes: Map<String, StrokeState> get() = _mainStrokes
    val outlineStrokes: Map<String, StrokeState> get() = _outlineStrokes
    val highlightStrokes: Map<String, StrokeState> get() = _highlightStrokes

    init {
        for (i in 0 until character.strokeCount) {
            val key = i.toString()
            _mainStrokes[key] = StrokeState(1.0, 1.0)
            _outlineStrokes[key] = StrokeState(1.0, 1.0)
            _highlightStrokes[key] = StrokeState(0.0, 1.0)
        }
    }

    fun setMainOpacity(opacity: Double) {
        mainOpacity = opacity.coerceIn(0.0, 1.0)
        notifyChange()
    }

    fun setOutlineOpacity(opacity: Double) {
        outlineOpacity = opacity.coerceIn(0.0, 1.0)
        notifyChange()
    }

    fun setHighlightOpacity(opacity: Double) {
        highlightOpacity = opacity.coerceIn(0.0, 1.0)
        notifyChange()
    }

    fun setStrokeOpacity(layer: String, strokeKey: String, opacity: Double) {
        val strokes = strokesForLayer(layer) ?: return
        val existing = strokes[strokeKey]
        strokes[strokeKey] = StrokeState(
            opacity = opacity.coerceIn(0.0, 1.0),
            displayPortion = existing?.displayPortion ?: 1.0
        )
        notifyChange()
    }

    fun setStrokeDisplayPortion(layer: String, strokeKey: String, displayPortion: Double) {
        val strokes = strokesForLayer(layer) ?: return
        val existing = strokes[strokeKey]
        strokes[strokeKey] = StrokeState(
            opacity = existing?.opacity ?: 0.0,
            displayPortion = displayPortion.coerceIn(0.0, 1.0)
        )
        notifyChange()
    }

    private fun strokesForLayer(layer: String): MutableMap<String, StrokeState>? = when (layer) {
        "main" -> _mainStrokes
        "outline" -> _outlineStrokes
        "highlight" -> _highlightStrokes
        else -> null
    }

    private fun notifyChange() {
        _stateFlow.value = this
    }
}
