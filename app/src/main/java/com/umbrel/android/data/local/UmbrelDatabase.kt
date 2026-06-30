package com.umbrel.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.umbrel.android.data.local.dao.CachedAppDao
import com.umbrel.android.data.local.dao.CachedFileDao
import com.umbrel.android.data.local.dao.CachedSystemDao
import com.umbrel.android.data.local.entity.CachedApp
import com.umbrel.android.data.local.entity.CachedSystemInfo
import com.umbrel.android.data.local.entity.CachedHardware
import com.umbrel.android.data.local.entity.CachedFileEntry

@Database(
    entities = [
        CachedApp::class,
        CachedSystemInfo::class,
        CachedHardware::class,
        CachedFileEntry::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class UmbrelDatabase : RoomDatabase() {
    abstract fun cachedAppDao(): CachedAppDao
    abstract fun cachedSystemDao(): CachedSystemDao
    abstract fun cachedFileDao(): CachedFileDao
}
