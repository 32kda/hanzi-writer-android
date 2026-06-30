package com.hanziwriter.app.ui.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hanziwriter.app.ui.components.DrawableStroke
import com.hanziwriter.app.ui.components.WritingCanvas

@Composable
fun SessionScreenContent(
    state: LearnUiState,
    onStrokeStart: (Offset) -> Unit,
    onStrokeMove: (Offset) -> Unit,
    onStrokeEnd: () -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit = {},
    onSkipDemo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (state.isComplete) {
        SessionResultContent(
            state = state,
            onDismiss = onDismiss
        )
        return
    }

    if (state.demoState != null) {
        val character = state.character ?: return
        val demoStrokes = character.strokes.mapIndexed { i, stroke ->
            val opacity = when {
                i < state.demoState.strokeIndex -> 1f
                i == state.demoState.strokeIndex -> 1f
                else -> 0.15f
            }
            val drawPortion = when {
                i < state.demoState.strokeIndex -> 1f
                i == state.demoState.strokeIndex -> state.demoState.progress
                else -> 1f
            }
            DrawableStroke(
                segments = stroke.getParsedPath() ?: emptyList(),
                medianPoints = stroke.points,
                color = Color.DarkGray,
                opacity = opacity,
                drawPortion = drawPortion,
                strokeNum = stroke.strokeNum
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = character.pinyin,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 4.dp)
            )

            WritingCanvas(
                character = character,
                referenceStrokes = demoStrokes,
                userStrokes = emptyList(),
                currentUserPoints = emptyList(),
                showNumbers = true,
                currentStrokeIndex = state.demoState.strokeIndex,
                animationProgress = 1f,
                onStrokeStart = null,
                onStrokeMove = null,
                onStrokeEnd = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onSkipDemo,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Next")
            }
        }
        return
    }

    if (state.isLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Text("Loading character...", modifier = Modifier.padding(top = 16.dp))
        }
        return
    }

    val character = state.character ?: return

    val referenceStrokes = if (state.renderState != null) {
        character.strokes.map { stroke ->
            val key = stroke.strokeNum.toString()
            val mainState = state.renderState.mainStrokes[key]
            DrawableStroke(
                segments = stroke.getParsedPath() ?: emptyList(),
                medianPoints = stroke.points,
                color = Color.DarkGray,
                opacity = mainState?.opacity?.toFloat() ?: 0f,
                drawPortion = mainState?.displayPortion?.toFloat() ?: 1f,
                strokeNum = stroke.strokeNum
            )
        }
    } else emptyList()

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = character.pinyin,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            text = character.definition,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(8.dp))

        WritingCanvas(
            character = character,
            referenceStrokes = referenceStrokes,
            userStrokes = state.userStrokes,
            currentUserPoints = state.currentUserPoints,
            showNumbers = state.showNumbers,
            currentStrokeIndex = state.currentStrokeIndex,
            animationProgress = 1f,
            onStrokeStart = onStrokeStart,
            onStrokeMove = onStrokeMove,
            onStrokeEnd = onStrokeEnd,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Stroke ${state.currentStrokeIndex + 1} of ${character.strokeCount}",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onBack) {
                Text("Back")
            }
        }
    }
}

@Composable
private fun SessionResultContent(
    state: LearnUiState,
    onDismiss: () -> Unit
) {
    val items = state.sessionCharacters.mapNotNull { char ->
        val unicode = char.symbol.codePointAt(0)
        val result = state.sessionResults[unicode]
        if (result != null) {
            val pct = if (result.totalAttempts > 0)
                result.correctAttempts * 100 / result.totalAttempts else 0
            Triple(char, result, pct)
        } else null
    }

    val totalCorrect = state.sessionResults.values.sumOf { it.correctAttempts }
    val totalAttempts = state.sessionResults.values.sumOf { it.totalAttempts }
    val overallPct = if (totalAttempts > 0) totalCorrect * 100 / totalAttempts else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(16.dp)
    ) {
        Text(
            text = "Session Complete!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$totalCorrect / $totalAttempts strokes correct ($overallPct%)",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${items.size} character(s) trained",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            for ((char, result, pct) in items) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = char.symbol,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.width(48.dp)
                    )

                    Text(
                        text = char.pinyin,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(80.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${result.correctAttempts}/${result.totalAttempts} strokes",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        LinearProgressIndicator(
                            progress = { pct / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Text(
                        text = "$pct%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                HorizontalDivider()
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Done")
        }
    }
}

@Composable
fun LearnScreen(
    unicodes: List<Int>,
    onComplete: () -> Unit,
    onBack: () -> Unit,
    viewModel: LearnSessionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(unicodes) {
        viewModel.startSession(unicodes)
    }

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) {
            viewModel.endSession()
            viewModel.playLessonCompleteSound()
        }
    }

    SessionScreenContent(
        state = state,
        onStrokeStart = { offset -> viewModel.onStrokeStart(offset) },
        onStrokeMove = { offset -> viewModel.onStrokeMove(offset) },
        onStrokeEnd = { viewModel.onStrokeEnd() },
        onBack = onBack,
        onDismiss = onComplete,
        onSkipDemo = { viewModel.skipDemo() }
    )
}
