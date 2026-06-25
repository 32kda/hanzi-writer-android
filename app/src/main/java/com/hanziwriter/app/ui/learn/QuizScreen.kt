package com.hanziwriter.app.ui.learn

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun QuizScreen(
    unicodes: List<Int>,
    onComplete: (Int) -> Unit,
    onBack: () -> Unit,
    viewModel: QuizSessionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(unicodes) {
        viewModel.startSession(unicodes)
    }

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) {
            viewModel.endSession()
            viewModel.playLessonCompleteSound()
            val totalCorrect = state.sessionResults.values.sumOf { it.correctAttempts }
            val totalAttempts = state.sessionResults.values.sumOf { it.totalAttempts }
            val score = if (totalAttempts > 0) (totalCorrect * 100 / totalAttempts) else 0
            onComplete(score)
        }
    }

    SessionScreenContent(
        state = state,
        onStrokeStart = { offset -> viewModel.onStrokeStart(offset) },
        onStrokeMove = { offset -> viewModel.onStrokeMove(offset) },
        onStrokeEnd = { viewModel.onStrokeEnd() },
        onBack = onBack
    )
}
