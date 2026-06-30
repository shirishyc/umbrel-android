package com.umbrel.android.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── System ───────────────────────────────────────────────────────────────

@Serializable
data class SystemStatus(
    val version: String? = null,
    @SerialName("versionName") val versionName: String? = null,
    @SerialName("updateAvailable") val updateAvailable: Boolean = false,
    @SerialName("updateVersion") val updateVersion: String? = null,
    @SerialName("updateDescription") val updateDescription: String? = null,
    val uptime: Long = 0,
    @SerialName("isUmbrelHome") val isUmbrelHome: Boolean? = null,
    @SerialName("isUmbrelPro") val isUmbrelPro: Boolean? = null,
    val hostname: String? = null,
)

@Serializable
data class SoftwareUpdate(
    @SerialName("updateAvailable") val updateAvailable: Boolean = false,
    @SerialName("latestVersion") val latestVersion: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("releaseUrl") val releaseUrl: String? = null,
)

// ─── Hardware ─────────────────────────────────────────────────────────────

@Serializable
data class HardwareInfo(
    val cpu: CpuInfo? = null,
    val memory: MemoryInfo? = null,
    val disk: DiskInfo? = null,
    @SerialName("cpuTemperature") val cpuTemperature: Double? = null,
)

@Serializable
data class CpuInfo(
    val model: String? = null,
    val load: Double? = null,
    val cores: Int? = null,
    val temperature: Double? = null,
)

@Serializable
data class MemoryInfo(
    val total: Long = 0,
    val used: Long = 0,
    val available: Long = 0,
    val percentage: Double? = null,
)

@Serializable
data class DiskInfo(
    val total: Long = 0,
    val used: Long = 0,
    val available: Long = 0,
    val percentage: Double? = null,
    val mount: String? = null,
)

// ─── Apps ─────────────────────────────────────────────────────────────────

@Serializable
data class AppInfo(
    val id: String = "",
    val title: String? = null,
    val author: String? = null,
    val description: String? = null,
    val version: String? = null,
    val state: String? = null,
    @SerialName("dockerStatus") val dockerStatus: String? = null,
    val icon: String? = null,
    val port: Int? = null,
    val category: String? = null,
    @SerialName("hidden") val hidden: Boolean = false,
    val website: String? = null,
    val license: String? = null,
)

@Serializable
data class AppState(
    val state: String = "unknown",
    val progress: Int = 0,
)

@Serializable
data class AppStoreRegistry(
    val categories: List<AppCategory> = emptyList(),
    val apps: List<AppInfo> = emptyList(),
)

@Serializable
data class AppCategory(
    val id: String = "",
    val name: String = "",
    val icon: String? = null,
    val description: String? = null,
)

@Serializable
data class AppInstallation(
    @SerialName("appId") val appId: String = "",
    val state: String = "",
    val progress: Int = 0,
)

// ─── User ─────────────────────────────────────────────────────────────────

@Serializable
data class UserProfile(
    val name: String? = null,
    val language: String? = null,
    val wallpaper: String? = null,
    @SerialName("temperatureUnit") val temperatureUnit: String? = null,
    @SerialName("hasPassword") val hasPassword: Boolean = true,
    @SerialName("is2faEnabled") val is2faEnabled: Boolean = false,
)

// ─── WiFi ─────────────────────────────────────────────────────────────────

@Serializable
data class WifiNetwork(
    val ssid: String = "",
    @SerialName("signalStrength") val signalStrength: Int? = null,
    val security: String? = null,
    val active: Boolean = false,
)

@Serializable
data class WifiConnectionStatus(
    val ssid: String? = null,
    val status: String? = null,
    val ip: String? = null,
    val signal: Int? = null,
)

// ─── File System ──────────────────────────────────────────────────────────

@Serializable
data class FileEntry(
    val name: String = "",
    val path: String = "",
    val type: String = "", // "file" or "directory"
    val size: Long = 0,
    @SerialName("modifiedAt") val modifiedAt: Long = 0,
    val mime: String? = null,
    @SerialName("thumbnail") val thumbnail: String? = null,
)

@Serializable
data class FileInfo(
    val name: String = "",
    val path: String = "",
    val type: String = "",
    val size: Long = 0,
    val mime: String? = null,
    @SerialName("modifiedAt") val modifiedAt: Long = 0,
)

// ─── Backups ──────────────────────────────────────────────────────────────

@Serializable
data class Backup(
    val id: String = "",
    val path: String? = null,
    val size: Long = 0,
    @SerialName("createdAt") val createdAt: Long = 0,
    val status: String? = null,
)

// ─── Notifications ────────────────────────────────────────────────────────

@Serializable
data class Notification(
    val id: String = "",
    val title: String? = null,
    val body: String? = null,
    val type: String? = null,
    val timestamp: Long = 0,
    @SerialName("isRead") val isRead: Boolean = false,
)

// ─── Shortcuts ────────────────────────────────────────────────────────────

@Serializable
data class Shortcut(
    val url: String = "",
    val title: String = "",
    val icon: String? = null,
)
