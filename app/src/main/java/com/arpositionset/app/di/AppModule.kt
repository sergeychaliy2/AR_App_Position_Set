package com.arpositionset.app.di

import com.arpositionset.app.core.DefaultDispatcher
import com.arpositionset.app.core.IoDispatcher
import com.arpositionset.app.core.MainDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton @IoDispatcher
    fun provideIo(): CoroutineDispatcher = Dispatchers.IO

    @Provides @Singleton @DefaultDispatcher
    fun provideDefault(): CoroutineDispatcher = Dispatchers.Default

    @Provides @Singleton @MainDispatcher
    fun provideMain(): CoroutineDispatcher = Dispatchers.Main.immediate
}
