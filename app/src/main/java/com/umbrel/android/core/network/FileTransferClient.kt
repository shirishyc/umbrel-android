package com.umbrel.android.core.network

import javax.inject.Qualifier

/**
 * Qualifier for the OkHttpClient used for large file transfers (longer timeouts).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FileTransferClient
