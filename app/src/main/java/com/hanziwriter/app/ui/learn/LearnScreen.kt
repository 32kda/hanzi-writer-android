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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hanziwriter.app.ui.components.WritingCanvas
import com.hanziwriter.app.ui.components.DrawableStroke

@Composable
fun LearnScreen(
    unicodes: List<Int>,
    onComplete: () -> Unit,
    onBack: () -> Unit,
    viewModel: LearnViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Start learning when screen appears (once per list of unicodes)
    androidx.compose.runtime.LaunchedEffect(unicodes) {
        viewModel.startLearn(unicodes)
    }

    // Navigate on complete
    androidx.compose.runtime.LaunchedEffect(state.isComplete) {
        if (state.isComplete) {
            viewModel.playLessonCompleteSound()
            onComplete()
        }
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
            val mainState = state.renderState!!.mainStrokes[key]
            DrawableStroke(
                svgPath = stroke.path,
                medianPoints = stroke.points,
                color = Color.DarkGray,
                opacity = mainState?.opacity?.toFloat() ?: 0f,
                drawPortion = mainState?.displayPortion?.toFloat() ?: 1f,
                strokeNum = stroke.strokeNum
            )
        }
    } else emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Character info header: pinyin + definition
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

        // Writing canvas
        WritingCanvas(
            character = character,
            referenceStrokes = referenceStrokes,
            userStrokes = state.userStrokes,
            currentUserPoints = state.currentUserPoints,
            showNumbers = state.showNumbers,
            currentStrokeIndex = state.currentStrokeIndex,
            animationProgress = 1f,
            onStrokeStart = { offset -> viewModel.onStrokeStart(offset) },
            onStrokeMove = { offset -> viewModel.onStrokeMove(offset) },
            onStrokeEnd = { viewModel.onStrokeEnd() },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Info + controls
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
