package com.vamshi.field.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for persisting and observing the active user session.
 *
 * The implementation (in the data layer) stores the current user ID in
 * SharedPreferences and exposes it as a [Flow] so the auth gate can react
 * to sign-in / sign-out without polling.
 */
interface SessionManager {

    /**
     * A hot [Flow] that emits the current user ID (or `null` for no session)
     * and re-emits whenever [setCurrentUserId] changes the stored value.
     *
     * Always emits the current value immediately on first collection (warm start).
     */
    fun observeCurrentUserId(): Flow<String?>

    /**
     * Persists [id] as the active user session.
     *
     * Pass `null` to clear the session (equivalent to sign-out).
     */
    suspend fun setCurrentUserId(id: String?)

    /**
     * One-shot read of the current user ID — useful for start-destination
     * decisions that don't need reactive updates.
     */
    suspend fun currentUserIdOnce(): String?
}
