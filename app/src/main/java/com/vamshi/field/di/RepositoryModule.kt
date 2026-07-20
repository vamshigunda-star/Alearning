package com.example.alearning.di

import com.example.alearning.data.repository.PeopleRepositoryImpl
import com.example.alearning.data.repository.PendingTestEntryRepositoryImpl
import com.example.alearning.data.repository.ReportsRepositoryImpl
import com.example.alearning.data.repository.StandardsRepositoryImpl
import com.example.alearning.data.repository.TestingRepositoryImpl
import com.example.alearning.domain.repository.PeopleRepository
import com.example.alearning.domain.repository.PendingTestEntryRepository
import com.example.alearning.domain.repository.ReportsRepository
import com.example.alearning.domain.repository.StandardsRepository
import com.example.alearning.domain.repository.TestingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPeopleRepository(
        peopleRepositoryImpl: PeopleRepositoryImpl
    ): PeopleRepository

    @Binds
    @Singleton
    abstract fun bindStandardsRepository(
        standardsRepositoryImpl: StandardsRepositoryImpl
    ): StandardsRepository

    @Binds
    @Singleton
    abstract fun bindTestingRepository(
        testingRepositoryImpl: TestingRepositoryImpl
    ): TestingRepository

    @Binds
    @Singleton
    abstract fun bindReportsRepository(
        reportsRepositoryImpl: ReportsRepositoryImpl
    ): ReportsRepository

    @Binds
    @Singleton
    abstract fun bindPendingTestEntryRepository(
        pendingTestEntryRepositoryImpl: PendingTestEntryRepositoryImpl
    ): PendingTestEntryRepository

    @Binds
    @Singleton
    abstract fun bindAiCoachRepository(
        aiCoachRepositoryImpl: com.example.alearning.data.repository.AiCoachRepositoryImpl
    ): com.example.alearning.domain.repository.AiCoachRepository

}