package com.hanziwriter.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.hanziwriter.app.data.local.AppPreferences
import com.hanziwriter.app.ui.navigation.NavGraph
import com.hanziwriter.app.ui.theme.HanziWriterTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Read the previously selected character set (null = first launch)
        val savedSetName = appPreferences.selectedSetName

        setContent {
            HanziWriterTheme {
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
