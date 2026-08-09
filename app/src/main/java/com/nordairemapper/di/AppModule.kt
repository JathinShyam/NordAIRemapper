package com.nordairemapper.di

import android.content.Context
import androidx.room.Room
import com.nordairemapper.data.datastore.SettingsRepositoryImpl
import com.nordairemapper.data.local.ConfigSnapshotDao
import com.nordairemapper.data.local.NordDatabase
import com.nordairemapper.data.local.OverlayConfigDao
import com.nordairemapper.data.local.RemapConfigDao
import com.nordairemapper.data.repository.RemapConfigRepositoryImpl
import com.nordairemapper.domain.repository.RemapConfigRepository
import com.nordairemapper.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NordDatabase =
        Room.databaseBuilder(context, NordDatabase::class.java, "nord_remapper.db").build()

    @Provides
    fun provideRemapConfigDao(db: NordDatabase): RemapConfigDao = db.remapConfigDao()

    @Provides
    fun provideOverlayConfigDao(db: NordDatabase): OverlayConfigDao = db.overlayConfigDao()

    @Provides
    fun provideConfigSnapshotDao(db: NordDatabase): ConfigSnapshotDao = db.configSnapshotDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindRemapConfigRepository(impl: RemapConfigRepositoryImpl): RemapConfigRepository

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
