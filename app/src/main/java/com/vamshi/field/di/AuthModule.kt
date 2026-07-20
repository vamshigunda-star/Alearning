package com.vamshi.field.di

import com.vamshi.field.data.auth.PasswordHasher
import com.vamshi.field.data.repository.AuthRepositoryImpl
import com.vamshi.field.data.repository.SessionManagerImpl
import com.vamshi.field.domain.repository.AuthRepository
import com.vamshi.field.domain.repository.SessionManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSessionManager(
        impl: SessionManagerImpl
    ): SessionManager

    companion object {
        /**
         * [PasswordHasher] is a pure JVM singleton — no Android context needed.
         * Provided here so it can be injected into [AuthRepositoryImpl].
         */
        @Provides
        @Singleton
        fun providePasswordHasher(): PasswordHasher = PasswordHasher()
    }
}
