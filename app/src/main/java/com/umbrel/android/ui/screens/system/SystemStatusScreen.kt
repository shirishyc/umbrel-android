package com.umbrel.android.ui.screens.system

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

@Composable
fun SystemStatusScreen(
    navController: NavController,
    viewModel: SystemStatusViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(text = "System Status", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            // Hardware section
            uiState.hardware?.let { hw ->
                Text(text = "Hardware", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                hw.cpu?.let { cpu ->
                    HardwareCard(
                        icon = Icons.Default.Memory,
                        label = "CPU",
                        detail = cpu.model ?: "",
                        value = cpu.load?.let { "${(it * 100).toInt()}%" } ?: "N/A",
                        progress = cpu.load?.toFloat() ?: 0f,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                hw.memory?.let { mem ->
                    val pct = if (mem.total > 0) mem.used.toFloat() / mem.total else 0f
                    HardwareCard(
                        icon = Icons.Default.Storage,
                        label = "Memory",
                        detail = "${mem.used / 1_000_000_000}GB / ${mem.total / 1_000_000_000}GB",
                        value = "${(pct * 100).toInt()}%",
                        progress = pct,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                hw.disk?.let { disk ->
                    val pct = if (disk.total > 0) disk.used.toFloat() / disk.total else 0f
                    HardwareCard(
                        icon = Icons.Default.Storage,
                        label = "Disk",
                        detail = "${disk.used / 1_000_000_000}GB / ${disk.total / 1_000_000_000}GB",
                        value = "${(pct * 100).toInt()}%",
                        progress = pct,
                    )
                }

                hw.cpuTemperature?.let { temp ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Thermostat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.padding(start = 8.dp))
                            Column { Text("CPU Temp"); Text("$temp°C", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Software update
            Text(text = "Software", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Update, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.padding(start = 8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(uiState.systemStatus?.versionName ?: "UmbrelOS")
                        Text("v${uiState.systemStatus?.version ?: "?"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (uiState.systemStatus?.updateAvailable == true) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.applyUpdate() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Update, contentDescription = null)
                    Spacer(modifier = Modifier.padding(start = 4.dp))
                    Text("Apply Update")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Controls
            Text(text = "Controls", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { viewModel.reboot() }, modifier = Modifier.fillMaxWidth()) {
                Text("Reboot")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.shutdown() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Shutdown")
            }
        }
    }
}

@Composable
private fun HardwareCard(
    icon: ImageVector,
    label: String,
    detail: String,
    value: String,
    progress: Float,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(label)
                    Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(value, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                strokeCap = StrokeCap.Round,
            )
        }
    }
}
