package com.umbrel.android.ui.screens.files

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.umbrel.android.data.models.FileEntry
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePreviewScreen(
    filePath: String,
    navController: NavController,
    viewModel: FilesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Find the file entry from the current directory listing
    val fileEntry = uiState.entries.find { it.path == filePath }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(fileEntry?.name ?: filePath) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    fileEntry?.let { entry ->
                        IconButton(onClick = {
                            viewModel.downloadFile(entry)
                        }) {
                            Icon(Icons.Default.Download, "Download")
                        }
                        IconButton(onClick = {
                            // Share file via Android share sheet
                            val url = viewModel.getFileUrl(entry)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = entry.mime ?: "*/*"
                                putExtra(Intent.EXTRA_TEXT, url)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share"))
                        }) {
                            Icon(Icons.Default.Share, "Share")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            if (fileEntry != null) {
                val imageTypes = setOf("image/jpeg", "image/png", "image/webp", "image/gif", "image/avif", "image/bmp")

                if (fileEntry.mime in imageTypes) {
                    AsyncImage(
                        model = viewModel.getFileUrl(fileEntry),
                        contentDescription = fileEntry.name,
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Text(
                        text = "Preview not available for this file type.\nTap download to save it.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text("File not found")
            }
        }
    }
}
