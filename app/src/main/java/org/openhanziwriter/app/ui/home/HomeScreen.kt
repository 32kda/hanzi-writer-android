package org.openhanziwriter.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.openhanziwriter.app.R
import org.openhanziwriter.app.ui.components.resolve

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            if (state.hasValidSet) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.clickable { onChangeSet() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(state.setDisplayName)
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = stringResource(R.string.home_change_set),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!state.hasValidSet) {
                Text(
                    text = stringResource(R.string.home_no_set),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.home_no_set_body),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onChangeSet) {
                    Text(stringResource(R.string.home_choose_set))
                }
            return@Scaffold
        }

        Text(
            text = state.streakText.resolve(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onViewCalendar() }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = state.engagementText.resolve(),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(onClick = onChangeSet) {
                Text(stringResource(R.string.home_change_set))
            }

            Spacer(modifier = Modifier.height(24.dp))

            ActivityCard(
                title = stringResource(R.string.home_learn),
                description = stringResource(R.string.home_learn_desc),
                chars = state.learnCharacters,
                color = MaterialTheme.colorScheme.primary,
                onClick = { if (state.nextLearningChars.isNotEmpty()) onNavigateToLearn(state.nextLearningChars) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ActivityCard(
                title = stringResource(R.string.home_drill),
                description = stringResource(R.string.home_drill_desc),
                chars = state.drillCharacters,
                color = MaterialTheme.colorScheme.secondary,
                onClick = { if (state.nextReviewChars.isNotEmpty()) onNavigateToDrill(state.nextReviewChars) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ActivityCard(
                title = stringResource(R.string.home_quiz),
                description = stringResource(R.string.home_quiz_desc),
                chars = emptyList(),
                color = MaterialTheme.colorScheme.error,
                onClick = { if (state.nextQuizChars.isNotEmpty()) onNavigateToQuiz(state.nextQuizChars) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityCard(
    title: String,
    description: String,
    chars: List<org.openhanziwriter.app.domain.model.quiz.QuizCard>,
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
