package com.umbrel.android.ui.screens.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.umbrel.android.data.models.AppInfo
import com.umbrel.android.ui.theme.AppStateInstalling
import com.umbrel.android.ui.theme.AppStateRunning
import com.umbrel.android.ui.theme.AppStateStopped
import com.umbrel.android.ui.theme.AppStateUnknown

@Composable
fun MyAppsScreen(
    navController: NavController,
    viewModel: MyAppsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "My Apps", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.apps) { app ->
                    AppCard(
                        app = app,
                        onStart = { viewModel.startApp(app.id) },
                        onStop = { viewModel.stopApp(app.id) },
                        onRestart = { viewModel.restartApp(app.id) },
                        onUninstall = { viewModel.uninstallApp(app.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppCard(
    app: AppInfo,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onUninstall: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.title ?: app.id, style = MaterialTheme.typography.titleSmall)
                    Text(app.version ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = app.state ?: "unknown",
                    color = when (app.state) {
                        "running" -> AppStateRunning
                        "stopped" -> AppStateStopped
                        "installing", "updating" -> AppStateInstalling
                        else -> AppStateUnknown
                    },
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                when (app.state) {
                    "running" -> {
                        Button(onClick = onStop, modifier = Modifier.weight(1f)) { Text("Stop") }
                        Button(onClick = onRestart, modifier = Modifier.weight(1f)) { Text("Restart") }
                    }
                    "stopped" -> {
                        Button(onClick = onStart, modifier = Modifier.weight(1f)) { Text("Start") }
                    }
                    "unknown" -> {
                        Button(onClick = onStart, modifier = Modifier.weight(1f)) { Text("Start") }
                    }
                }
                Button(
                    onClick = onUninstall,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Uninstall")
                }
            }
        }
    }
}
