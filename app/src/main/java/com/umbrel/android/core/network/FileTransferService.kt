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
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody

/**
 * Handles file uploads and downloads via the UmbrelOS Files REST API.
 *
 * Uses cookie-based auth (UMBREL_PROXY_TOKEN), handled automatically
 * by the shared OkHttp CookieJar.
 */
@Singleton
class FileTransferService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private var baseUrl: String = ""

    fun configure(url: String) {
        baseUrl = url.trimEnd('/')
    }

    fun getBaseUrl(): String = baseUrl

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

            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            downloadsDir.mkdirs()
            val file = File(downloadsDir, fileName)

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

    fun getDownloadUrl(virtualPath: String): String {
        return "$baseUrl/api/files/download?path=$virtualPath"
    }

    fun getThumbnailUrl(virtualPath: String): String {
        return "$baseUrl/api/files/thumbnail/$virtualPath"
    }
}
