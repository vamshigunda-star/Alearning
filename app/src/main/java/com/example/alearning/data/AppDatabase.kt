package com.example.alearning.data

import androidx.room.Database
import androidx.room.RoomDatabase
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
    version = 3
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun peopleDao(): PeopleDao
    abstract fun standardsDao(): StandardsDao
    abstract fun testingDao(): TestingDao
}
