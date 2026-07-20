package com.vamshi.field.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.vamshi.field.data.auth.PasswordHasher
import com.vamshi.field.data.local.daos.auth.UserDao
import com.vamshi.field.data.local.entities.auth.UserEntity
import com.vamshi.field.data.mapper.auth.toDomain
import com.vamshi.field.domain.model.auth.AuthError
import com.vamshi.field.domain.model.auth.AuthResult
import com.vamshi.field.domain.model.auth.User
import com.vamshi.field.domain.repository.AuthRepository
import com.vamshi.field.domain.repository.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [AuthRepository].
 *
 * Responsibilities:
 *  - Normalize usernames (trim + lowercase) before every DB operation.
 *  - Hash passwords and security answers via [PasswordHasher] (PBKDF2-SHA256).
 *  - Manage session via [SessionManager] (SharedPreferences).
 *  - Map [SQLiteConstraintException] to [AuthError.UsernameTaken].
 *  - Never expose raw entities — always map through [toDomain] before returning.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    private val hasher: PasswordHasher
) : AuthRepository {

    override suspend fun signUp(
        firstName: String,
        lastName: String,
        username: String,
        password: String,
        email: String?,
        securityQuestion: String?,
        securityAnswer: String?
    ): AuthResult = withContext(Dispatchers.IO) {
        val normalizedUsername = username.trim().lowercase()

        val (pwdSalt, pwdHash) = hasher.hash(password)
        // Security-answer hashing is skipped entirely (null hash/salt persisted) when no
        // security answer is supplied — the current onboarding path never sets one.
        val (ansSalt, ansHash) = if (!securityAnswer.isNullOrBlank()) {
            hasher.hash(securityAnswer.trim().lowercase())
        } else {
            null to null
        }

        val entity = UserEntity(
            id = UUID.randomUUID().toString(),
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            username = normalizedUsername,
            email = email?.trim()?.ifBlank { null },
            passwordHash = pwdHash,
            passwordSalt = pwdSalt,
            securityQuestion = securityQuestion?.trim(),
            securityAnswerHash = ansHash,
            securityAnswerSalt = ansSalt,
            createdAt = System.currentTimeMillis()
        )

        return@withContext try {
            userDao.insert(entity)
            // Auto sign-in: set the session immediately after account creation.
            sessionManager.setCurrentUserId(entity.id)
            AuthResult.Success(entity.toDomain())
        } catch (e: SQLiteConstraintException) {
            AuthResult.Failure(AuthError.UsernameTaken)
        } catch (e: Exception) {
            AuthResult.Failure(AuthError.Unknown)
        }
    }

    override suspend fun signIn(username: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            val normalizedUsername = username.trim().lowercase()
            val entity = userDao.getByUsername(normalizedUsername)
                ?: return@withContext AuthResult.Failure(AuthError.InvalidCredentials)

            val valid = hasher.verify(password, entity.passwordSalt, entity.passwordHash)
            if (!valid) {
                return@withContext AuthResult.Failure(AuthError.InvalidCredentials)
            }

            sessionManager.setCurrentUserId(entity.id)
            AuthResult.Success(entity.toDomain())
        }

    override suspend fun signOut() {
        sessionManager.setCurrentUserId(null)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCurrentUser(): Flow<User?> =
        sessionManager.observeCurrentUserId().flatMapLatest { id ->
            if (id == null) {
                flowOf(null)
            } else {
                userDao.observeById(id).map { it?.toDomain() }
            }
        }

    override suspend fun isUsernameTaken(username: String): Boolean =
        withContext(Dispatchers.IO) {
            userDao.getByUsername(username.trim().lowercase()) != null
        }

    override suspend fun userCount(): Int = withContext(Dispatchers.IO) {
        userDao.count()
    }

    override suspend fun resetPassword(
        username: String,
        securityAnswer: String,
        newPassword: String
    ): AuthResult = withContext(Dispatchers.IO) {
        val normalizedUsername = username.trim().lowercase()
        val normalizedAnswer = securityAnswer.trim().lowercase()

        val entity = userDao.getByUsername(normalizedUsername)
            ?: return@withContext AuthResult.Failure(AuthError.UsernameNotFound)

        val ansHash = entity.securityAnswerHash
        val ansSalt = entity.securityAnswerSalt
        if (ansHash == null || ansSalt == null) {
            return@withContext AuthResult.Failure(AuthError.NoSecurityQuestion)
        }

        val answerValid = hasher.verify(normalizedAnswer, ansSalt, ansHash)
        if (!answerValid) {
            return@withContext AuthResult.Failure(AuthError.IncorrectSecurityAnswer)
        }

        val (newSalt, newHash) = hasher.hash(newPassword)
        userDao.updatePasswordHash(entity.id, newHash, newSalt)
        AuthResult.Success(entity.toDomain())
    }

    override suspend fun getSecurityQuestion(username: String): String? =
        withContext(Dispatchers.IO) {
            userDao.getByUsername(username.trim().lowercase())?.securityQuestion
        }

    override suspend fun getPrimaryAccount(): User? = withContext(Dispatchers.IO) {
        // userDao.getAll() is already ordered by createdAt DESC, so the head of the
        // list satisfies both the "single account" and "most recent" cases.
        userDao.getAll().firstOrNull()?.toDomain()
    }

    override suspend fun listAccounts(): List<User> = withContext(Dispatchers.IO) {
        userDao.getAll().map { it.toDomain() }
    }

    override suspend fun unlock(userId: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            val entity = userDao.getById(userId)
                ?: return@withContext AuthResult.Failure(AuthError.InvalidCredentials)

            val valid = hasher.verify(password, entity.passwordSalt, entity.passwordHash)
            if (!valid) {
                return@withContext AuthResult.Failure(AuthError.InvalidCredentials)
            }

            sessionManager.setCurrentUserId(entity.id)
            AuthResult.Success(entity.toDomain())
        }

    override suspend fun establishSessionAfterRestore(): AuthResult =
        withContext(Dispatchers.IO) {
            val user = userDao.getAll().firstOrNull()
                ?: return@withContext AuthResult.Failure(AuthError.Unknown)
            sessionManager.setCurrentUserId(user.id)
            AuthResult.Success(user.toDomain())
        }
}
