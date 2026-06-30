package com.umbrel.android.core.network

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FileTransferClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideCookieJar(): CookieJar {
        val cookieStore = ConcurrentHashMap<String, List<Cookie>>()
        return object : CookieJar {
            override fun saveFromResponse(url: okhttp3.HttpUrl, cookies: List<Cookie>) {
                cookieStore[url.host] = cookies
            }

            override fun loadForRequest(url: okhttp3.HttpUrl): List<Cookie> {
                return cookieStore[url.host] ?: emptyList()
            }
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(cookieJar: CookieJar): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (com.umbrel.android.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .cookieJar(cookieJar)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    @FileTransferClient
    fun provideFileTransferClient(cookieJar: CookieJar): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .followRedirects(true)
            .cookieJar(cookieJar)
            .build()
    }

    @Provides
    @Singleton
    fun provideTrpcClient(
        client: OkHttpClient,
        json: Json,
    ): TrpcClient = TrpcClient(client, json)

    @Provides
    @Singleton
    fun provideTrpcWebSocketClient(
        json: Json,
    ): TrpcWebSocketClient = TrpcWebSocketClient(json)

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        client: OkHttpClient,
    ): ImageLoader = ImageLoader.Builder(context)
        .okHttpClient { client }
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("coil_cache"))
                .maxSizePercent(0.02)
                .build()
        }
        .build()
}
