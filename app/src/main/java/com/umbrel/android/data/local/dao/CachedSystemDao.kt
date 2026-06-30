package com.umbrel.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.umbrel.android.data.local.entity.CachedSystemInfo
import com.umbrel.android.data.local.entity.CachedHardware
import com.umbrel.android.data.local.entity.CachedFileEntry

@Dao
interface CachedSystemDao {

    @Query("SELECT * FROM cached_system_info WHERE id = 1")
    suspend fun getSystemInfo(): CachedSystemInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSystemInfo(info: CachedSystemInfo)

    @Query("SELECT * FROM cached_hardware WHERE id = 1")
    suspend fun getHardware(): CachedHardware?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHardware(hardware: CachedHardware)
}

@Dao
interface CachedFileDao {

    @Query("SELECT * FROM cached_file_entries WHERE directoryPath = :dirPath ORDER BY type ASC, name ASC")
    suspend fun getFiles(dirPath: String): List<CachedFileEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<CachedFileEntry>)

    @Query("DELETE FROM cached_file_entries WHERE directoryPath = :dirPath")
    suspend fun clearDirectory(dirPath: String)
}
