package com.example.dexplorer.core.di

import android.content.Context
import com.example.dexplorer.data.repository.FileSystemRepositoryImpl
import com.example.dexplorer.domain.repository.FileSystemRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideFileSystemRepository(
        @ApplicationContext context: Context
    ): FileSystemRepository {
        return FileSystemRepositoryImpl(context)
    }
}