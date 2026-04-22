package com.arpositionset.app.di

import com.arpositionset.app.data.repository.ObjectRepositoryImpl
import com.arpositionset.app.data.repository.PlacementRepositoryImpl
import com.arpositionset.app.data.repository.ScenePersistenceRepositoryImpl
import com.arpositionset.app.data.repository.ScenePreferencesRepositoryImpl
import com.arpositionset.app.domain.repository.ObjectRepository
import com.arpositionset.app.domain.repository.PlacementRepository
import com.arpositionset.app.domain.repository.ScenePersistenceRepository
import com.arpositionset.app.domain.repository.ScenePreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindObjectRepository(impl: ObjectRepositoryImpl): ObjectRepository

    @Binds @Singleton
    abstract fun bindPlacementRepository(impl: PlacementRepositoryImpl): PlacementRepository

    @Binds @Singleton
    abstract fun bindScenePreferences(impl: ScenePreferencesRepositoryImpl): ScenePreferencesRepository

    @Binds @Singleton
    abstract fun bindScenePersistence(impl: ScenePersistenceRepositoryImpl): ScenePersistenceRepository

    @Binds @Singleton
    abstract fun bindSceneBinding(impl: com.arpositionset.app.data.repository.SceneBindingRepositoryImpl): com.arpositionset.app.domain.repository.SceneBindingRepository
}
