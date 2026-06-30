package com.umbrel.android.data.local

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.umbrel.android.data.local.dao.CachedAppDao
import com.umbrel.android.data.local.dao.CachedFileDao
import com.umbrel.android.data.local.dao.CachedSystemDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): UmbrelDatabase {
        return Room.databaseBuilder(
            context,
            UmbrelDatabase::class.java,
            "umbrel_cache.db",
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideCachedAppDao(db: UmbrelDatabase): CachedAppDao = db.cachedAppDao()

    @Provides
    fun provideCachedSystemDao(db: UmbrelDatabase): CachedSystemDao = db.cachedSystemDao()

    @Provides
    fun provideCachedFileDao(db: UmbrelDatabase): CachedFileDao = db.cachedFileDao()
}
