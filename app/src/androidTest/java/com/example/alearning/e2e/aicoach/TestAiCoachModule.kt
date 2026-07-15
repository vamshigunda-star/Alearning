package com.example.alearning.e2e.aicoach

import com.example.alearning.domain.repository.AiCoachRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import dagger.hilt.testing.TestInstallIn
import com.example.alearning.di.RepositoryModule

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class]
)
abstract class TestAiCoachModule {

    @Binds
    @Singleton
    abstract fun bindAiCoachRepository(
        fakeAiCoachRepository: FakeAiCoachRepository
    ): AiCoachRepository
}
// Test comment
