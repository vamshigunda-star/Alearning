package com.vamshi.field.di

import com.vamshi.field.data.repository.AiCoachRepositoryImpl
import com.vamshi.field.domain.repository.AiCoachRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiCoachModule {

    @Binds
    @Singleton
    abstract fun bindAiCoachRepository(
        aiCoachRepositoryImpl: AiCoachRepositoryImpl
    ): AiCoachRepository
}
