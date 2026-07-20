package com.vamshi.field.e2e.aicoach

import com.vamshi.field.domain.repository.AiCoachRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import dagger.hilt.testing.TestInstallIn
import com.vamshi.field.di.AiCoachModule

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AiCoachModule::class]
)
abstract class TestAiCoachModule {

    @Binds
    @Singleton
    abstract fun bindAiCoachRepository(
        fakeAiCoachRepository: FakeAiCoachRepository
    ): AiCoachRepository
}
// Test comment
