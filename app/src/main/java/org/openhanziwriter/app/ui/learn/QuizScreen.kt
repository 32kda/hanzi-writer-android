package org.openhanziwriter.app.ui.learn

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import org.openhanziwriter.app.R

@Composable
fun QuizScreen(
    unicodes: List<Int>,
    onComplete: () -> Unit,
    onBack: () -> Unit,
    viewModel: QuizSessionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(unicodes) {
        viewModel.startSession(unicodes)
    }

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) {
            viewModel.endSession().join()
            viewModel.playLessonCompleteSound()
        }
    }

    SessionScreenContent(
        state = state,
        title = stringResource(R.string.session_quiz),
        onStrokeStart = { offset -> viewModel.onStrokeStart(offset) },
        onStrokeMove = { offset -> viewModel.onStrokeMove(offset) },
        onStrokeEnd = { viewModel.onStrokeEnd() },
        onBack = onBack,
        onDismiss = onComplete
    )
}
