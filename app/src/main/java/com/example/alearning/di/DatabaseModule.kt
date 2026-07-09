package com.example.alearning.di

import android.content.Context
import androidx.room.Room
import com.example.alearning.data.AppDatabase
import com.example.alearning.data.local.daos.auth.UserDao
import com.example.alearning.data.local.daos.people.PeopleDao
import com.example.alearning.data.local.daos.standards.StandardsDao
import com.example.alearning.data.local.daos.testing.PendingTestEntryDao
import com.example.alearning.data.local.daos.testing.TestingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "alearning-db"
        )
            .addMigrations(
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9
            )
            .build()
    }

    @Provides
    fun providePeopleDao(db: AppDatabase): PeopleDao = db.peopleDao()

    @Provides
    fun provideStandardsDao(db: AppDatabase): StandardsDao = db.standardsDao()

    @Provides
    fun provideTestingDao(db: AppDatabase): TestingDao = db.testingDao()

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun providePendingTestEntryDao(db: AppDatabase): PendingTestEntryDao = db.pendingTestEntryDao()
}
