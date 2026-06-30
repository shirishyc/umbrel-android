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
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

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
            level = if (true) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        // Custom DNS resolver that handles .local (mDNS) addresses
        val mdnsDns = object : Dns {
            override fun lookup(hostname: String): List<java.net.InetAddress> {
                return try {
                    Dns.SYSTEM.lookup(hostname)
                } catch (e: java.net.UnknownHostException) {
                    if (hostname.endsWith(".local")) {
                        java.net.InetAddress.getAllByName(hostname).toList()
                    } else {
                        throw e
                    }
                }
            }
        }

        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .cookieJar(cookieJar)
            .dns(mdnsDns)
            .addInterceptor(logging)
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
    ): ImageLoader = ImageLoader.Builder(context)
        .memoryCache {
            MemoryCache.Builder(context)
                .maxSizePercent(0.25)
                .build()
        }
        .build()
}
