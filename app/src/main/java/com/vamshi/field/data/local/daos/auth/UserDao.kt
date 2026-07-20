package com.vamshi.field.data.local.daos.auth

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vamshi.field.data.local.entities.auth.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for coach account persistence.
 *
 * All mutating operations are suspend functions; all observation operations
 * return [Flow] so Room can push updates reactively.
 */
@Dao
interface UserDao {

    /**
     * Inserts a new user. If a duplicate [UserEntity.username] is inserted,
     * the unique index will throw [android.database.sqlite.SQLiteConstraintException],
     * which [com.vamshi.field.data.repository.AuthRepositoryImpl] catches and
     * maps to [com.vamshi.field.domain.model.auth.AuthError.UsernameTaken].
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity)

    /** Looks up a user by their normalized username. Null if not found. */
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getByUsername(username: String): UserEntity?

    /** Looks up a user by primary key. Null if not found. */
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): UserEntity?

    /**
     * Reactive stream for a specific user — used to keep the session
     * observation alive even if the user entity is updated.
     */
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<UserEntity?>

    /** Returns the total number of stored accounts. */
    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    /**
     * Updates the password credentials for an existing user during a
     * password reset operation.
     */
    @Query(
        "UPDATE users SET passwordHash = :hash, passwordSalt = :salt WHERE id = :id"
    )
    suspend fun updatePasswordHash(id: String, hash: ByteArray, salt: ByteArray)
}
