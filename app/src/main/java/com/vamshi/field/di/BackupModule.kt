package com.vamshi.field.di

import com.vamshi.field.data.repository.BackupRepositoryImpl
import com.vamshi.field.domain.repository.BackupRepository
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
