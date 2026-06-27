package com.hanziwriter.app.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner

@Composable
fun HomeScreen(
    onNavigateToLearn: (List<Int>) -> Unit,
    onNavigateToDrill: (List<Int>) -> Unit,
    onNavigateToQuiz: (List<Int>) -> Unit,
    onViewCalendar: () -> Unit,
    onChangeSet: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSelections()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!state.hasValidSet) {
            Text(
                text = "No character set selected",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "The previously selected set is no longer available. Please choose a different set.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onChangeSet) {
                Text("Choose a Set")
            }
            return
        }

        Text(
            text = state.setDisplayName,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        TextButton(onClick = onViewCalendar) {
            Text(
                text = state.streakText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = state.engagementText,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(4.dp))

        Button(onClick = onChangeSet) {
            Text("Change Set")
        }

        Spacer(modifier = Modifier.height(24.dp))

        ActivityCard(
            title = "Learn",
            description = "2-3 new characters",
            chars = state.learnCharacters,
            color = MaterialTheme.colorScheme.primary,
            onClick = { if (state.nextLearningChars.isNotEmpty()) onNavigateToLearn(state.nextLearningChars) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ActivityCard(
            title = "Drill",
            description = "5 review characters",
            chars = state.drillCharacters,
            color = MaterialTheme.colorScheme.secondary,
            onClick = { if (state.nextReviewChars.isNotEmpty()) onNavigateToDrill(state.nextReviewChars) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ActivityCard(
            title = "Quiz",
            description = "10 characters",
            chars = emptyList(),
            color = MaterialTheme.colorScheme.error,
            onClick = { if (state.nextQuizChars.isNotEmpty()) onNavigateToQuiz(state.nextQuizChars) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityCard(
    title: String,
    description: String,
    chars: List<com.hanziwriter.app.domain.model.quiz.QuizCard>,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (chars.isNotEmpty()) {
                Text(
                    text = chars.joinToString(" · ") { card ->
                        "${card.character} (${card.pinyin})"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
