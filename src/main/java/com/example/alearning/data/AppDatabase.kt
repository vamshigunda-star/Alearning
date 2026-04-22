package com.example.alearning.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.alearning.data.local.Converters
import com.example.alearning.data.local.daos.people.PeopleDao
import com.example.alearning.data.local.daos.standards.StandardsDao
import com.example.alearning.data.local.daos.testing.TestingDao
import com.example.alearning.data.local.entities.people.GroupEntity
import com.example.alearning.data.local.entities.people.GroupMemberCrossRef
import com.example.alearning.data.local.entities.people.IndividualEntity
import com.example.alearning.data.local.entities.standards.FitnessTestEntity
import com.example.alearning.data.local.entities.standards.NormReferenceEntity
import com.example.alearning.data.local.entities.standards.TestCategoryEntity
import com.example.alearning.data.local.entities.testing.EventTestCrossRef
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
        EventTestCrossRef::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun peopleDao(): PeopleDao
    abstract fun standardsDao(): StandardsDao
    abstract fun testingDao(): TestingDao

    companion object {
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
