package com.umbrel.android.core.network

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Handles file uploads and downloads via the UmbrelOS Files REST API.
 *
 * Uses cookie-based auth (UMBREL_PROXY_TOKEN), which is automatically
 * handled by the OkHttp CookieJar.
 *
 * REST endpoints:
 *   GET  /api/files/download?path=<virtual_path>
 *   POST /api/files/upload?path=<virtual_path>
 *   GET  /api/files/thumbnail/<hash>
 */
@Singleton
class FileTransferService @Inject constructor(
    @FileTransferClient private val client: OkHttpClient,
    @ApplicationContext private val context: Context,
) {
    private var baseUrl: String = ""

    fun configure(url: String) {
        baseUrl = url.trimEnd('/')
    }

    /**
     * Download a file from UmbrelOS to the device's Downloads folder.
     * Returns the local file path on success.
     */
    suspend fun downloadFile(virtualPath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$baseUrl/api/files/download?path=$virtualPath")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("Download failed: HTTP ${response.code}")
            }

            val body = response.body ?: throw Exception("Empty response body")
            val fileName = virtualPath.substringAfterLast("/").ifBlank { "download" }

            // Save to device Downloads folder
            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            downloadsDir.mkdirs()
            val file = File(downloadsDir, fileName)

            // Handle duplicate filenames
            var outputFile = file
            var counter = 1
            while (outputFile.exists()) {
                val nameWithoutExt = fileName.substringBeforeLast(".")
                val ext = fileName.substringAfterLast(".", "")
                outputFile = if (ext.isNotEmpty()) {
                    File(downloadsDir, "${nameWithoutExt}_($counter).$ext")
                } else {
                    File(downloadsDir, "${nameWithoutExt}_($counter)")
                }
                counter++
            }

            body.byteStream().use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Notify MediaStore on newer Android versions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, outputFile.name)
                    put(MediaStore.Downloads.IS_PENDING, 0)
                }
                context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values,
                )
            }

            outputFile.absolutePath
        }
    }

    /**
     * Upload a file to UmbrelOS.
     * @param virtualPath The destination path on the Umbrel (e.g. "Home/uploads")
     * @param localFile The local file to upload
     */
    suspend fun uploadFile(virtualPath: String, localFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val mimeType = MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(localFile.extension)
                ?: "application/octet-stream"

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    localFile.name,
                    localFile.asRequestBody(mimeType.toMediaTypeOrNull()),
                )
                .build()

            val request = Request.Builder()
                .url("$baseUrl/api/files/upload?path=$virtualPath")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("Upload failed: HTTP ${response.code}")
            }
        }
    }

    /**
     * Get the download URL for a file (for use with Coil/Glide for image loading).
     */
    fun getDownloadUrl(virtualPath: String): String {
        return "$baseUrl/api/files/download?path=$virtualPath"
    }

    /**
     * Get the thumbnail URL for a file.
     */
    fun getThumbnailUrl(virtualPath: String): String {
        return "$baseUrl/api/files/thumbnail/$virtualPath"
    }

    /**
     * Get the base URL.
     */
    fun getBaseUrl(): String = baseUrl
}
