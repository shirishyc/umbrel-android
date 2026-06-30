package com.umbrel.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.umbrel.android.data.local.entity.CachedApp
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedAppDao {

    @Query("SELECT * FROM cached_apps ORDER BY title ASC")
    suspend fun getAll(): List<CachedApp>

    @Query("SELECT * FROM cached_apps ORDER BY title ASC")
    fun observeAll(): Flow<List<CachedApp>>

    @Query("SELECT * FROM cached_apps WHERE id = :appId")
    suspend fun getById(appId: String): CachedApp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<CachedApp>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: CachedApp)

    @Query("DELETE FROM cached_apps")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM cached_apps WHERE cached_at > :since")
    suspend fun countFresh(since: Long): Int
}
