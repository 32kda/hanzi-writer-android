package org.openhanziwriter.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import org.openhanziwriter.app.data.local.AppPreferences
import org.openhanziwriter.app.ui.navigation.NavGraph
import org.openhanziwriter.app.ui.theme.OpenHanziWriterTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_OpenHanziWriter)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Read the previously selected character set (null = first launch)
        val savedSetName = appPreferences.selectedSetName

        setContent {
            OpenHanziWriterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph(
                        savedSetName = savedSetName,
                        // Persist the chosen set so next launch skips the selector
                        onSelectSet = { setName -> appPreferences.selectedSetName = setName }
                    )
                }
            }
        }
    }
}
