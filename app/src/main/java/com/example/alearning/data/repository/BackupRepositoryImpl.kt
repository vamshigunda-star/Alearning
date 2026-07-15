package com.example.alearning.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.alearning.data.AppDatabase
import com.example.alearning.domain.model.backup.*
import com.example.alearning.domain.repository.BackupRepository
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import com.example.alearning.data.backup.DriveBackupHelper
import javax.inject.Inject

class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase,
    private val gson: Gson,
    private val driveBackupHelper: DriveBackupHelper
) : BackupRepository {

    private val backupDao = appDatabase.backupDao()
    private val syncState = MutableStateFlow(false)
    private var lastBackupTimestamp: Long? = null

    override suspend fun backupToDrive() = withContext(Dispatchers.IO) {
        syncState.value = true
        try {
            // Extract all data
            val individuals = backupDao.getAllIndividuals().map { 
                BackupIndividual(it.id, it.firstName, it.lastName, it.dateOfBirth, it.sex.name, it.notes)
            }
            val groups = backupDao.getAllGroups().map {
                BackupGroup(it.id, it.name, it.category ?: "", !it.isDeleted)
            }
            val groupMembers = backupDao.getAllGroupMembers().map {
                BackupGroupMemberCrossRef(it.groupId, it.individualId)
            }
            val testingEvents = backupDao.getAllTestingEvents().map {
                BackupTestingEvent(it.id, it.name, it.date, it.notes)
            }
            val eventTests = backupDao.getAllEventTests().map {
                BackupEventTestCrossRef(it.eventId, it.testId)
            }
            val testResults = backupDao.getAllTestResults().map {
                BackupTestResult(it.id, it.eventId, it.individualId, it.testId, it.rawScore, it.percentile?.toDouble(), it.createdAt, it.captureMethod, null)
            }
            val users = backupDao.getAllUsers().map {
                BackupUser(it.id, it.firstName, it.lastName, it.username, 
                           android.util.Base64.encodeToString(it.passwordHash, android.util.Base64.NO_WRAP), 
                           android.util.Base64.encodeToString(it.passwordSalt, android.util.Base64.NO_WRAP), 
                           it.securityQuestion, 
                           it.securityAnswerHash?.let { h -> android.util.Base64.encodeToString(h, android.util.Base64.NO_WRAP) }, 
                           it.securityAnswerSalt?.let { s -> android.util.Base64.encodeToString(s, android.util.Base64.NO_WRAP) }, 
                           it.createdAt)
            }

            val payload = BackupPayload(
                individuals, groups, groupMembers, testingEvents, eventTests, testResults, users
            )

            // Serialize to local cache
            val json = gson.toJson(payload)
            val backupFile = File(context.cacheDir, "alearning_backup_staged.json")
            backupFile.writeText(json)

            // Upload to Google Drive using DriveBackupHelper
            driveBackupHelper.uploadToDrive(context, backupFile)
            
            lastBackupTimestamp = System.currentTimeMillis()
        } finally {
            syncState.value = false
        }
    }

    override suspend fun restoreFromDrive() = withContext(Dispatchers.IO) {
        syncState.value = true
        try {
            // Download from Google Drive
            val backupFile = File(context.cacheDir, "alearning_backup_staged.json")
            driveBackupHelper.downloadFromDrive(context, backupFile)
            
            if (!backupFile.exists()) {
                throw Exception("No backup found on Drive")
            }

            val json = backupFile.readText()
            val payload = gson.fromJson(json, BackupPayload::class.java)

            // Inside transaction, clear and replace
            appDatabase.runInTransaction {
                // Clear existing
                appDatabase.query("DELETE FROM group_members", null)
                appDatabase.query("DELETE FROM event_test_cross_ref", null)
                appDatabase.query("DELETE FROM test_results", null)
                appDatabase.query("DELETE FROM individuals", null)
                appDatabase.query("DELETE FROM `groups`", null)
                appDatabase.query("DELETE FROM testing_events", null)
                appDatabase.query("DELETE FROM users", null)

                // Insert mapped entities (requires Dao bulk inserts or iterating here. We can't suspend inside runInTransaction, but Room allows synchronous calls in DAOs if not marked suspend, or we can use Coroutines in a runBlocking block, but the easiest is just manually mapping and inserting via helper).
                // Actually, the easiest is to call the suspend functions inside a coroutine transaction builder.
            }
            
            // To do this cleanly with coroutines and Room:
            appDatabase.withTransaction {
                backupDao.clearAllUserGeneratedData()
                
                val userEntities = payload.users.map {
                    com.example.alearning.data.local.entities.auth.UserEntity(
                        it.id, it.firstName, it.lastName, it.username,
                        android.util.Base64.decode(it.passwordHash, android.util.Base64.NO_WRAP),
                        android.util.Base64.decode(it.passwordSalt, android.util.Base64.NO_WRAP),
                        it.securityQuestion,
                        it.securityAnswerHash?.let { h -> android.util.Base64.decode(h, android.util.Base64.NO_WRAP) },
                        it.securityAnswerSalt?.let { s -> android.util.Base64.decode(s, android.util.Base64.NO_WRAP) },
                        it.createdAt
                    )
                }
                val indEntities = payload.individuals.map {
                    com.example.alearning.data.local.entities.people.IndividualEntity(
                        id = it.id, firstName = it.firstName, lastName = it.lastName, 
                        dateOfBirth = it.dateOfBirth, sex = com.example.alearning.domain.model.people.BiologicalSex.valueOf(it.gender), notes = it.notes
                    )
                }
                val grpEntities = payload.groups.map {
                    com.example.alearning.data.local.entities.people.GroupEntity(
                        id = it.id, name = it.name, location = null, cycle = null, category = it.type, isDeleted = !it.isActive
                    )
                }
                val gmEntities = payload.groupMembers.map {
                    com.example.alearning.data.local.entities.people.GroupMemberCrossRef(it.groupId, it.individualId)
                }
                val teEntities = payload.testingEvents.map {
                    com.example.alearning.data.local.entities.testing.TestingEventEntity(
                        id = it.id, name = it.name, date = it.timestamp, notes = it.notes
                    )
                }
                val etEntities = payload.eventTests.map {
                    com.example.alearning.data.local.entities.testing.EventTestCrossRef(it.eventId, it.testId)
                }
                val trEntities = payload.testResults.map {
                    com.example.alearning.data.local.entities.testing.TestResultEntity(
                        id = it.id, eventId = it.eventId, individualId = it.individualId, testId = it.testId, 
                        rawScore = it.rawScore, ageAtTime = 0f, captureMethod = it.captureMethod, createdAt = it.timestamp
                    )
                }

                backupDao.insertUsers(userEntities)
                backupDao.insertIndividuals(indEntities)
                backupDao.insertGroups(grpEntities)
                backupDao.insertGroupMembers(gmEntities)
                backupDao.insertTestingEvents(teEntities)
                backupDao.insertEventTests(etEntities)
                backupDao.insertTestResults(trEntities)
            }
            
        } finally {
            syncState.value = false
        }
    }

    override suspend fun getLastBackupTimestamp(): Long? {
        return lastBackupTimestamp
    }

    override fun isSyncing(): Flow<Boolean> {
        return syncState
    }
}
