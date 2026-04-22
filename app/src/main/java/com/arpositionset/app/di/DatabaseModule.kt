package com.arpositionset.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.arpositionset.app.data.local.ArDatabase
import com.arpositionset.app.data.local.dao.ObjectDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ArDatabase =
        Room.databaseBuilder(context, ArDatabase::class.java, ArDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideObjectDao(db: ArDatabase): ObjectDao = db.objectDao()

    @Provides
    fun providePlacementRecordDao(db: ArDatabase) = db.placementRecordDao()

    @Provides
    fun provideSceneBindingDao(db: ArDatabase) = db.sceneBindingDao()

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { File(context.filesDir, "datastore/scene.preferences_pb") },
        )
}
