package com.example.alearning.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.alearning.data.local.Converters
import com.example.alearning.data.local.daos.auth.UserDao
import com.example.alearning.data.local.daos.backup.BackupDao
import com.example.alearning.data.local.daos.people.PeopleDao
import com.example.alearning.data.local.daos.standards.StandardsDao
import com.example.alearning.data.local.daos.testing.PendingTestEntryDao
import com.example.alearning.data.local.daos.testing.TestingDao
import com.example.alearning.data.local.entities.auth.UserEntity
import com.example.alearning.data.local.entities.people.GroupEntity
import com.example.alearning.data.local.entities.people.GroupMemberCrossRef
import com.example.alearning.data.local.entities.people.IndividualEntity
import com.example.alearning.data.local.entities.standards.FitnessTestEntity
import com.example.alearning.data.local.entities.standards.NormReferenceEntity
import com.example.alearning.data.local.entities.standards.TestCategoryEntity
import com.example.alearning.data.local.entities.testing.EventTestCrossRef
import com.example.alearning.data.local.entities.testing.PendingTestEntryEntity
import com.example.alearning.data.local.entities.testing.TestResultEntity
import com.example.alearning.data.local.entities.testing.TestingEventEntity

@Database(
    entities = [
        IndividualEntity::class,
        GroupEntity::class,
        GroupMemberCrossRef::class,
        TestCategoryEntity::class,
        FitnessTestEntity::class,
        NormReferenceEntity::class,
        TestingEventEntity::class,
        TestResultEntity::class,
        EventTestCrossRef::class,
        UserEntity::class,
        PendingTestEntryEntity::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun peopleDao(): PeopleDao
    abstract fun standardsDao(): StandardsDao
    abstract fun testingDao(): TestingDao
    abstract fun userDao(): UserDao
    abstract fun pendingTestEntryDao(): PendingTestEntryDao
    abstract fun backupDao(): BackupDao

    companion object {
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE fitness_tests ADD COLUMN youtubeId TEXT DEFAULT NULL")
            }
        }

        /**
         * Migration 7 → 8: Adds the `pending_test_entries` table.
         *
         * Holds in-flight scores staged during a TestingGrid session before the coach taps
         * "Submit All". Source of truth for pending state — survives process death so a
         * coach mid-session can resume after a kill/force-quit without losing entries.
         *
         * Composite PK (eventId, individualId, testId) — natural key, enforces one pending
         * score per athlete-test-per-event. FK to testing_events with CASCADE so deleting
         * an event cleans up any orphaned pending state.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pending_test_entries` (
                        `eventId` TEXT NOT NULL,
                        `individualId` TEXT NOT NULL,
                        `testId` TEXT NOT NULL,
                        `rawScore` REAL NOT NULL,
                        `stagedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`eventId`, `individualId`, `testId`),
                        FOREIGN KEY(`eventId`) REFERENCES `testing_events`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_pending_test_entries_eventId` ON `pending_test_entries` (`eventId`)"
                )
            }
        }

        /**
         * Migration 6 → 7: Adds the `users` table for coach authentication.
         *
         * Columns:
         *  - id            TEXT PK
         *  - firstName     TEXT NOT NULL
         *  - lastName      TEXT NOT NULL
         *  - username      TEXT NOT NULL (unique index enforces uniqueness)
         *  - passwordHash  BLOB NOT NULL (PBKDF2 256-bit derived key)
         *  - passwordSalt  BLOB NOT NULL (16-byte SecureRandom salt)
         *  - securityQuestion TEXT (nullable — not all accounts may have one)
         *  - securityAnswerHash BLOB (nullable)
         *  - securityAnswerSalt BLOB (nullable)
         *  - createdAt     INTEGER NOT NULL (epoch millis)
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `users` (
                        `id` TEXT NOT NULL,
                        `firstName` TEXT NOT NULL,
                        `lastName` TEXT NOT NULL,
                        `username` TEXT NOT NULL,
                        `passwordHash` BLOB NOT NULL,
                        `passwordSalt` BLOB NOT NULL,
                        `securityQuestion` TEXT,
                        `securityAnswerHash` BLOB,
                        `securityAnswerSalt` BLOB,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_users_username` ON `users` (`username`)"
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE fitness_tests ADD COLUMN validMin REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE fitness_tests ADD COLUMN validMax REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE fitness_tests ADD COLUMN interpretationStrategy TEXT NOT NULL DEFAULT 'NORM_LOOKUP'")
                db.execSQL("ALTER TABLE fitness_tests ADD COLUMN calculationConfig TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE test_categories ADD COLUMN radarAxis TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE fitness_tests ADD COLUMN inputParadigm TEXT NOT NULL DEFAULT 'NUMERIC'")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add stopwatch columns to fitness_tests
                db.execSQL("ALTER TABLE fitness_tests ADD COLUMN timingMode TEXT NOT NULL DEFAULT 'MANUAL_ENTRY'")
                db.execSQL("ALTER TABLE fitness_tests ADD COLUMN athletesPerHeat INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE fitness_tests ADD COLUMN trialsPerAthlete INTEGER NOT NULL DEFAULT 1")

                // Add capture method to test_results
                db.execSQL("ALTER TABLE test_results ADD COLUMN captureMethod TEXT NOT NULL DEFAULT 'MANUAL_ENTRY'")
            }
        }
    }
}
