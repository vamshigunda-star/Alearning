package com.example.alearning.di

import com.example.alearning.data.repository.BackupRepositoryImpl
import com.example.alearning.domain.repository.BackupRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {

    @Binds
    @Singleton
    abstract fun bindBackupRepository(
        backupRepositoryImpl: BackupRepositoryImpl
    ): BackupRepository

    companion object {
        @dagger.Provides
        @Singleton
        fun provideGson(): com.google.gson.Gson {
            return com.google.gson.Gson()
        }
    }
}
