package com.vamshi.field.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.vamshi.field.data.AppDatabase
import com.vamshi.field.data.backup.DeviceIdentifier
import com.vamshi.field.data.backup.DriveBackupHelper
import com.vamshi.field.data.local.entities.people.IndividualEntity
import com.vamshi.field.domain.model.backup.BackupGroup
import com.vamshi.field.domain.model.backup.BackupIndividual
import com.vamshi.field.domain.model.backup.BackupPayload
import com.vamshi.field.domain.model.people.BiologicalSex
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Regression coverage for the restore-from-Drive data-loss bug: a malformed backup payload
 * used to be applied via two separate transactions (a raw-SQL clear that committed on its own,
 * then a second transaction that mapped and inserted). If mapping threw, the clear had already
 * committed and local data was gone with nothing restored. [BackupRepositoryImpl.restoreEntities]
 * now does the clear and the insert in one atomic transaction.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BackupRepositoryImplTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: BackupRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = BackupRepositoryImpl(context, db, Gson(), DriveBackupHelper(DeviceIdentifier(context)))
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedOneIndividual(): IndividualEntity {
        val existing = IndividualEntity(
            id = "existing-1",
            firstName = "Original",
            lastName = "Athlete",
            dateOfBirth = 0L,
            sex = BiologicalSex.MALE
        )
        db.backupDao().insertIndividuals(listOf(existing))
        return existing
    }

    @Test
    fun `restoreEntities with an unparseable field leaves existing local data untouched`() = runTest {
        val existing = seedOneIndividual()

        // "not-a-real-sex" doesn't match any BiologicalSex constant, so BiologicalSex.valueOf
        // throws mid-mapping inside the transaction — exactly what a corrupted or
        // schema-mismatched backup file produces.
        val corruptPayload = BackupPayload(
            individuals = listOf(
                BackupIndividual(
                    id = "restored-1",
                    firstName = "Restored",
                    lastName = "Athlete",
                    dateOfBirth = 0L,
                    gender = "not-a-real-sex",
                    notes = null
                )
            ),
            groups = emptyList(),
            groupMembers = emptyList(),
            testingEvents = emptyList(),
            eventTests = emptyList(),
            testResults = emptyList(),
            users = emptyList()
        )

        try {
            repository.restoreEntities(corruptPayload)
            throw AssertionError("Expected restoreEntities to throw on an invalid payload")
        } catch (e: IllegalArgumentException) {
            // expected: BiologicalSex.valueOf("not-a-real-sex") failing
        }

        val remaining = db.backupDao().getAllIndividuals()
        assertEquals(
            "Local data must survive a failed restore instead of being wiped",
            listOf(existing),
            remaining
        )
    }

    @Test
    fun `restoreEntities with a valid payload replaces local data`() = runTest {
        seedOneIndividual()

        val payload = BackupPayload(
            individuals = listOf(
                BackupIndividual(
                    id = "restored-1",
                    firstName = "Restored",
                    lastName = "Athlete",
                    dateOfBirth = 12345L,
                    gender = BiologicalSex.FEMALE.name,
                    notes = "from backup"
                )
            ),
            groups = listOf(BackupGroup(id = "g1", name = "Team A", type = "TEAM", isActive = true)),
            groupMembers = emptyList(),
            testingEvents = emptyList(),
            eventTests = emptyList(),
            testResults = emptyList(),
            users = emptyList()
        )

        repository.restoreEntities(payload)

        val individuals = db.backupDao().getAllIndividuals()
        assertEquals(1, individuals.size)
        assertEquals("restored-1", individuals.first().id)
        assertEquals(1, db.backupDao().getAllGroups().size)
    }

    @Test
    fun `getLastBackupTimestamp is null until a backup timestamp is recorded`() = runTest {
        assertNull(repository.getLastBackupTimestamp())
    }

    @Test
    fun `backup timestamp survives repository recreation, simulating process death`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        repository.recordBackupTimestamp(42_000L)

        // A fresh repository instance over the same app context reads the same SharedPreferences
        // file — this is what happens after the process is killed and relaunched.
        val recreated = BackupRepositoryImpl(context, db, Gson(), DriveBackupHelper(DeviceIdentifier(context)))

        assertEquals(42_000L, recreated.getLastBackupTimestamp())
    }
}
