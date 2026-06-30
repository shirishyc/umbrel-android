package com.umbrel.android.data.local

import com.umbrel.android.data.api.AppsApi
import com.umbrel.android.data.api.FilesApi
import com.umbrel.android.data.api.HardwareApi
import com.umbrel.android.data.api.SystemApi
import com.umbrel.android.data.local.dao.CachedAppDao
import com.umbrel.android.data.local.dao.CachedFileDao
import com.umbrel.android.data.local.dao.CachedSystemDao
import com.umbrel.android.data.local.entity.CachedApp
import com.umbrel.android.data.local.entity.CachedFileEntry
import com.umbrel.android.data.local.entity.CachedHardware
import com.umbrel.android.data.local.entity.CachedSystemInfo
import com.umbrel.android.data.models.AppInfo
import com.umbrel.android.data.models.FileEntry
import com.umbrel.android.data.models.HardwareInfo
import com.umbrel.android.data.models.SystemStatus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cache TTLs in milliseconds.
 */
object CacheTtl {
    /** App list — refresh every 5 minutes */
    val APPS: Long = 5 * 60 * 1000
    /** System status — refresh every 2 minutes */
    val SYSTEM: Long = 2 * 60 * 1000
    /** Hardware stats — refresh every 30 seconds */
    val HARDWARE: Long = 30 * 1000
    /** File listings — refresh every 1 minute */
    val FILES: Long = 60 * 1000
}

/**
 * Cache-first repository that wraps API calls with local Room caching.
 *
 * Strategy:
 * 1. On read -> check local cache. If fresh (under TTL), return cached data immediately.
 * 2. Simultaneously, fetch from network in the background.
 * 3. If network succeeds -> update cache + return fresh data.
 * 4. If network fails -> return cached data (even if stale) rather than nothing.
 */
@Singleton
class CacheRepository @Inject constructor(
    private val cachedAppDao: CachedAppDao,
    private val cachedSystemDao: CachedSystemDao,
    private val cachedFileDao: CachedFileDao,
    private val appsApi: AppsApi,
    private val systemApi: SystemApi,
    private val hardwareApi: HardwareApi,
    private val filesApi: FilesApi,
) {

    // ─── Apps ────────────────────────────────────────────────────────────

    suspend fun getApps(forceRefresh: Boolean = false): Result<List<AppInfo>> {
        val now = System.currentTimeMillis()
        val cached = cachedAppDao.getAll()

        // Return cached if fresh and no force refresh
        if (!forceRefresh && cached.isNotEmpty()) {
            val isFresh = cached.firstOrNull()?.let { now - it.cachedAt < CacheTtl.APPS } ?: false
            if (isFresh) {
                return Result.success(cached.map { it.toAppInfo() })
            }
        }

        // Fetch from network
        return appsApi.list().also { result ->
            result.onSuccess { apps ->
                cachedAppDao.clearAll()
                cachedAppDao.insertAll(apps.map { it.toCachedApp() })
            }
        }.recoverIfCached(cached) { it.map { it.toAppInfo() } }
    }

    // ─── System Status ───────────────────────────────────────────────────

    suspend fun getSystemStatus(forceRefresh: Boolean = false): Result<SystemStatus> {
        val cached = cachedSystemDao.getSystemInfo()

        if (!forceRefresh && cached != null) {
            val isFresh = System.currentTimeMillis() - cached.cachedAt < CacheTtl.SYSTEM
            if (isFresh) {
                return Result.success(cached.toSystemStatus())
            }
        }

        return systemApi.getStatus().also { result ->
            result.onSuccess { status ->
                cachedSystemDao.insertSystemInfo(status.toCached())
            }
        }.recoverIfCached(cached) { it.toSystemStatus() }
    }

    // ─── Hardware ────────────────────────────────────────────────────────

    suspend fun getHardware(forceRefresh: Boolean = false): Result<HardwareInfo> {
        val cached = cachedSystemDao.getHardware()

        if (!forceRefresh && cached != null) {
            val isFresh = System.currentTimeMillis() - cached.cachedAt < CacheTtl.HARDWARE
            if (isFresh) {
                return Result.success(cached.toHardwareInfo())
            }
        }

        return hardwareApi.get().also { result ->
            result.onSuccess { hw ->
                cachedSystemDao.insertHardware(hw.toCached())
            }
        }.recoverIfCached(cached) { it.toHardwareInfo() }
    }

    // ─── Files ───────────────────────────────────────────────────────────

    suspend fun getFiles(path: String, forceRefresh: Boolean = false): Result<List<FileEntry>> {
        val now = System.currentTimeMillis()
        val cached = cachedFileDao.getFiles(path)

        if (!forceRefresh && cached.isNotEmpty()) {
            val isFresh = now - cached.first().cachedAt < CacheTtl.FILES
            if (isFresh) {
                return Result.success(cached.map { it.toFileEntry() })
            }
        }

        return filesApi.list(path).also { result ->
            result.onSuccess { entries ->
                cachedFileDao.clearDirectory(path)
                cachedFileDao.insertFiles(entries.map { it.toCached(path) })
            }
        }.recoverIfCached(cached) { it.map { it.toFileEntry() } }
    }

    /** Fallback: if cached data exists, return it even if stale, rather than propagating an error */
    private suspend fun <T, C> Result<T>.recoverIfCached(cached: C?, toData: (C) -> T): Result<T> {
        return if (this.isFailure && cached != null) {
            Result.success(toData(cached))
        } else {
            this
        }
    }

    // ─── Mapping extensions (private to class) ─────────────────────────────

    private fun AppInfo.toCachedApp() = CachedApp(
        id = id,
        title = title,
        author = author,
        description = description,
        version = version,
        state = state,
        icon = icon,
        category = category,
        dockerStatus = dockerStatus,
        port = port,
    )

    private fun CachedApp.toAppInfo() = AppInfo(
        id = id,
        title = title,
        author = author,
        description = description,
        version = version,
        state = state,
        icon = icon,
        category = category,
        dockerStatus = dockerStatus,
        port = port,
    )

    private fun SystemStatus.toCached() = CachedSystemInfo(
        version = version,
        versionName = versionName,
        updateAvailable = updateAvailable,
        updateVersion = updateVersion,
        uptime = uptime,
        hostname = hostname,
    )

    private fun CachedSystemInfo.toSystemStatus() = SystemStatus(
        version = version,
        versionName = versionName,
        updateAvailable = updateAvailable,
        updateVersion = updateVersion,
        uptime = uptime,
        hostname = hostname,
    )

    private fun HardwareInfo.toCached() = CachedHardware(
        cpuLoad = cpu?.load,
        cpuCores = cpu?.cores,
        cpuTemperature = cpuTemperature,
        memoryTotal = memory?.total ?: 0,
        memoryUsed = memory?.used ?: 0,
        memoryAvailable = memory?.available ?: 0,
        diskTotal = disk?.total ?: 0,
        diskUsed = disk?.used ?: 0,
        diskAvailable = disk?.available ?: 0,
    )

    private fun CachedHardware.toHardwareInfo() = HardwareInfo(
        cpu = if (cpuLoad != null) com.umbrel.android.data.models.CpuInfo(
            load = cpuLoad,
            cores = cpuCores,
            temperature = cpuTemperature,
        ) else null,
        memory = com.umbrel.android.data.models.MemoryInfo(
            total = memoryTotal,
            used = memoryUsed,
            available = memoryAvailable,
        ),
        disk = com.umbrel.android.data.models.DiskInfo(
            total = diskTotal,
            used = diskUsed,
            available = diskAvailable,
        ),
        cpuTemperature = cpuTemperature,
    )

    private fun FileEntry.toCached(directoryPath: String) = CachedFileEntry(
        path = this.path,
        directoryPath = directoryPath,
        name = name,
        type = type,
        size = size,
        mime = mime,
        modifiedAt = modifiedAt,
    )

    private fun CachedFileEntry.toFileEntry() = FileEntry(
        name = name,
        path = path,
        type = type,
        size = size,
        mime = mime,
        modifiedAt = modifiedAt,
    )
}