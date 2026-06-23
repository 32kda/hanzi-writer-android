package com.hanziwriter.app.ui.setselector

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetSelectorScreen(
    onSetSelected: (String) -> Unit,
    viewModel: SetSelectorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.previewImport(uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                importLauncher.launch(arrayOf("text/csv", "application/zip"))
            }) {
                Icon(Icons.Default.Add, contentDescription = "Import character set")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .padding(top = 16.dp)
        ) {
            Text(
                text = "Choose a Character Set",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (state.sets.isEmpty()) {
                Text(
                    text = "No character sets found. Tap + to import one.",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.sets, key = { it.dirName }) { set ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSetSelected(set.dirName) },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = set.displayName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (set.description.isNotBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = set.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (!set.isBuiltIn) {
                                    IconButton(onClick = { viewModel.deleteSet(set.dirName) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete set",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    when (val imp = importState) {
        is ImportState.Preview -> {
            if (imp.preview.collision != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissImport() },
                    title = { Text("Overwrite set?") },
                    text = {
                        Text("A set named '${imp.preview.name}' already exists. Overwrite?")
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.confirmImport(overwrite = true) }) {
                            Text("Overwrite")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissImport() }) {
                            Text("Cancel")
                        }
                    }
                )
            } else {
                LaunchedEffect(imp) {
                    viewModel.confirmImport(overwrite = false)
                }
            }
        }
        is ImportState.Importing -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Importing...") },
                text = { CircularProgressIndicator() },
                confirmButton = {}
            )
        }
        is ImportState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissImport() },
                title = { Text("Import failed") },
                text = { Text(imp.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissImport() }) {
                        Text("OK")
                    }
                }
            )
        }
        is ImportState.Idle -> {}
    }
}
