package com.umbrel.android.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_apps")
data class CachedApp(
    @PrimaryKey val id: String,
    val title: String? = null,
    val author: String? = null,
    val description: String? = null,
    val version: String? = null,
    val state: String? = null,
    val icon: String? = null,
    val category: String? = null,
    val dockerStatus: String? = null,
    val port: Int? = null,
    @ColumnInfo(name = "cached_at") val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "cached_system_info")
data class CachedSystemInfo(
    @PrimaryKey val id: Int = 1,
    val version: String? = null,
    val versionName: String? = null,
    val updateAvailable: Boolean = false,
    val updateVersion: String? = null,
    val uptime: Long = 0,
    val hostname: String? = null,
    @ColumnInfo(name = "cached_at") val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "cached_hardware")
data class CachedHardware(
    @PrimaryKey val id: Int = 1,
    val cpuLoad: Double? = null,
    val cpuCores: Int? = null,
    val cpuTemperature: Double? = null,
    val memoryTotal: Long = 0,
    val memoryUsed: Long = 0,
    val memoryAvailable: Long = 0,
    val diskTotal: Long = 0,
    val diskUsed: Long = 0,
    val diskAvailable: Long = 0,
    @ColumnInfo(name = "cached_at") val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "cached_file_entries")
data class CachedFileEntry(
    @PrimaryKey val path: String,
    val directoryPath: String = "",
    val name: String = "",
    val type: String = "",
    val size: Long = 0,
    val mime: String? = null,
    val modifiedAt: Long = 0,
    @ColumnInfo(name = "cached_at") val cachedAt: Long = System.currentTimeMillis(),
)
