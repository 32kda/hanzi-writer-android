package org.openhanziwriter.app.ui.learn

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import org.openhanziwriter.app.R
import org.openhanziwriter.app.ui.components.DrawableStroke
import org.openhanziwriter.app.ui.components.WritingCanvas

private const val DEMO_ANIM_SPEED = 1024

private const val ANIM_DELAY = 16L

private const val AFTER_ANIM_DELAY = 500L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreenContent(
    state: LearnUiState,
    title: String,
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

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.session_back))
                    }
                }
            )
        }
    ) { paddingValues ->

        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Text(stringResource(R.string.session_loading), modifier = Modifier.padding(top = 16.dp))
            }
            return@Scaffold
        }

        if (state.demoState != null) {
            val character = state.character ?: return@Scaffold

            var animStrokeIndex by remember { mutableStateOf(0) }
            var animProgress by remember { mutableStateOf(0f) }

            Log.d("DemoAnim", "Entered demo block, strokes=${character.strokes.size}")

            LaunchedEffect(state.demoState) {
                while (true) {
                    for (i in character.strokes.indices) {
                        animStrokeIndex = i
                        val strokeLen = character.strokes[i].length
                        val totalFrames = maxOf(5, (strokeLen / DEMO_ANIM_SPEED * 1000.0 / 16.0).toInt())
                        for (frame in 0..totalFrames) {
                            animProgress = frame.toFloat() / totalFrames
                            delay(ANIM_DELAY)
                        }
                    }
                    Log.d("DemoAnim", "Loop restart after 500ms")
                    delay(AFTER_ANIM_DELAY)
                }
            }

            val demoStrokes = character.strokes.mapIndexed { i, stroke ->
                val opacity = when {
                    i < animStrokeIndex -> 1f
                    i == animStrokeIndex -> 1f
                    else -> 0.15f
                }
                val drawPortion = when {
                    i < animStrokeIndex -> 1f
                    i == animStrokeIndex -> animProgress
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
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = character.pinyin,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 2.dp)
                )
                Text(
                    text = character.definition,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    currentStrokeIndex = animStrokeIndex,
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
                    Text(stringResource(R.string.session_next))
                }
            }
            return@Scaffold
        }

        val character = state.character ?: return@Scaffold

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
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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

            Text(
                text = stringResource(R.string.session_stroke_fmt, state.currentStrokeIndex + 1, character.strokeCount),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SessionResultContent(
    state: LearnUiState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
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
            text = stringResource(R.string.session_complete),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.session_score_fmt, totalCorrect, totalAttempts, overallPct),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        if (overallPct >= 50) {
            val fraction = ((overallPct - 50) / 50f).coerceIn(0f, 1f)
            val ratingColor = Color(red = 1f - fraction, green = 1f, blue = 0f)
            val ratingResId = when {
                overallPct >= 99 -> R.string.session_rating_unreal
                overallPct >= 90 -> R.string.session_rating_awesome
                overallPct >= 70 -> R.string.session_rating_excellent
                overallPct >= 60 -> R.string.session_rating_perfect
                else -> R.string.session_rating_good
            }
            Text(
                text = stringResource(ratingResId),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ratingColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = context.resources.getQuantityString(R.plurals.session_chars_trained, items.size, items.size),
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
                            text = context.resources.getQuantityString(R.plurals.session_per_char_result, result.totalAttempts, result.correctAttempts, result.totalAttempts),
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
            Text(stringResource(R.string.session_done))
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
            viewModel.endSession().join()
            viewModel.playLessonCompleteSound()
        }
    }

    SessionScreenContent(
        state = state,
        title = stringResource(R.string.session_learn),
        onStrokeStart = { offset -> viewModel.onStrokeStart(offset) },
        onStrokeMove = { offset -> viewModel.onStrokeMove(offset) },
        onStrokeEnd = { viewModel.onStrokeEnd() },
        onBack = onBack,
        onDismiss = onComplete,
        onSkipDemo = { viewModel.skipDemo() }
    )
}
