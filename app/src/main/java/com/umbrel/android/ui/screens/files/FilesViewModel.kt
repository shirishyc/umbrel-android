package com.umbrel.android.ui.screens.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.umbrel.android.data.api.FilesApi
import com.umbrel.android.core.network.FileTransferService
import com.umbrel.android.data.local.CacheRepository
import com.umbrel.android.data.models.FileEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FilesUiState(
    val isLoading: Boolean = true,
    val isDownloading: String? = null,
    val currentPath: String = "Home",
    val breadcrumbs: List<String> = listOf("Home"),
    val entries: List<FileEntry> = emptyList(),
    val downloadResult: String? = null,
    val error: String? = null,
)

@HiltViewModel
class FilesViewModel @Inject constructor(
    private val filesApi: FilesApi,
    private val cache: CacheRepository,
    private val fileTransfer: FileTransferService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FilesUiState())
    val uiState: StateFlow<FilesUiState> = _uiState.asStateFlow()

    init { loadDirectory("Home") }

    fun loadDirectory(path: String) {
        viewModelScope.launch {
            val crumbs = buildBreadcrumbs(path)
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                currentPath = path,
                breadcrumbs = crumbs,
            )
            cache.getFiles(path).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        entries = it,
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = it.message,
                    )
                },
            )
        }
    }

    fun navigateTo(path: String) { loadDirectory(path) }

    fun navigateUp() {
        val crumbs = _uiState.value.breadcrumbs
        if (crumbs.size > 1) {
            val parent = crumbs.dropLast(1).joinToString("/")
            loadDirectory(parent)
        }
    }

    fun downloadFile(entry: FileEntry) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = entry.path)
            fileTransfer.downloadFile(entry.path).fold(
                onSuccess = { path ->
                    _uiState.value = _uiState.value.copy(isDownloading = null, downloadResult = path)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isDownloading = null, error = "Download failed: ${e.message}")
                },
            )
        }
    }

    fun getThumbnailUrl(entry: FileEntry): String? {
        if (entry.type != "file") return null
        val imageTypes = setOf("image/jpeg", "image/png", "image/webp", "image/gif", "image/avif", "image/bmp")
        return if (entry.mime in imageTypes) fileTransfer.getDownloadUrl(entry.path) else null
    }

    fun getFileUrl(entry: FileEntry): String = fileTransfer.getDownloadUrl(entry.path)

    fun clearDownloadResult() { _uiState.value = _uiState.value.copy(downloadResult = null) }

    private fun buildBreadcrumbs(path: String): List<String> {
        val parts = path.split("/").filter { it.isNotBlank() }
        return if (parts.isEmpty()) listOf("Home") else parts
    }
}
