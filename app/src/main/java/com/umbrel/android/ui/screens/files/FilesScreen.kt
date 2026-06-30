package com.umbrel.android.ui.screens.files

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.umbrel.android.data.models.FileEntry

@Composable
fun FilesScreen(
    initialPath: String,
    viewModel: FilesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load initial path on first composition
    LaunchedEffect(initialPath) {
        viewModel.loadDirectory(initialPath)
    }

    // Show download success
    LaunchedEffect(uiState.downloadResult) {
        uiState.downloadResult?.let {
            snackbarHostState.showSnackbar("Downloaded to: $it")
            viewModel.clearDownloadResult()
        }
    }

    // Show errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("/${uiState.currentPath}") },
                navigationIcon = {
                    if (uiState.breadcrumbs.size > 1) {
                        IconButton(onClick = { viewModel.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Breadcrumb path
                item {
                    BreadcrumbRow(
                        crumbs = uiState.breadcrumbs,
                        onCrumbClick = { index ->
                            val path = uiState.breadcrumbs.take(index + 1).joinToString("/")
                            viewModel.loadDirectory(path)
                        },
                    )
                }

                items(uiState.entries) { entry ->
                    FileEntryCard(
                        entry = entry,
                        isDownloading = uiState.isDownloading == entry.path,
                        thumbnailUrl = viewModel.getThumbnailUrl(entry),
                        onClick = {
                            if (entry.type == "directory") {
                                viewModel.navigateTo(entry.path)
                            }
                        },
                        onDownload = { viewModel.downloadFile(entry) },
                    )
                }

                if (uiState.entries.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "This folder is empty",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun BreadcrumbRow(
    crumbs: List<String>,
    onCrumbClick: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        crumbs.forEachIndexed { index, crumb ->
            Text(
                text = if (index == crumbs.lastIndex) crumb else "$crumb /",
                style = if (index == crumbs.lastIndex) {
                    MaterialTheme.typography.titleSmall
                } else {
                    MaterialTheme.typography.bodySmall
                },
                color = if (index == crumbs.lastIndex) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.clickable { onCrumbClick(index) },
            )
        }
    }
}

@Composable
private fun FileEntryCard(
    entry: FileEntry,
    isDownloading: Boolean,
    thumbnailUrl: String?,
    onClick: () -> Unit,
    onDownload: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = entry.type == "directory") { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon or thumbnail
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = entry.name,
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = getFileIcon(entry),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).padding(4.dp),
                    tint = if (entry.type == "directory") {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            Spacer(modifier = Modifier.padding(start = 8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entry.size > 0) {
                    Text(
                        text = formatSize(entry.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Actions
            if (entry.type == "file" && !isDownloading) {
                IconButton(onClick = onDownload) {
                    Icon(Icons.Default.Download, "Download", tint = MaterialTheme.colorScheme.primary)
                }
            } else if (isDownloading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
    }
}

private fun getFileIcon(entry: FileEntry): ImageVector = when {
    entry.type == "directory" -> Icons.Default.Folder
    entry.mime?.startsWith("image/") == true -> Icons.Default.Image
    entry.mime?.startsWith("video/") == true -> Icons.Default.PlayArrow
    entry.mime?.startsWith("audio/") == true -> Icons.Default.MusicNote
    else -> Icons.Default.InsertDriveFile
}

internal fun formatSize(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "${"%.1f".format(bytes.toDouble() / 1_000_000_000)} GB"
    bytes >= 1_000_000 -> "${"%.1f".format(bytes.toDouble() / 1_000_000)} MB"
    bytes >= 1_000 -> "${"%.1f".format(bytes.toDouble() / 1_000)} KB"
    else -> "$bytes B"
}
