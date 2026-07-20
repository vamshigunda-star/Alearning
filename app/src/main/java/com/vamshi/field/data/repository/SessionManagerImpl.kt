package com.vamshi.field.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.vamshi.field.domain.repository.SessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SharedPreferences-backed implementation of [SessionManager].
 *
 * Storage:
 *  - File name: "alearning_prefs" (shared with SeedDataManager and CustomPresetsStore)
 *  - Key:       "current_user_id"
 *  - Value:     UUID string of the active user, or absent/null for no session
 *
 * Reactive observation is implemented with [callbackFlow] + [SharedPreferences.OnSharedPreferenceChangeListener].
 * The current value is emitted first on collection, then on every subsequent change.
 * [distinctUntilChanged] prevents duplicate emissions if unrelated keys change.
 */
@Singleton
class SessionManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SessionManager {

    companion object {
        private const val PREFS_NAME = "alearning_prefs"
        private const val KEY_CURRENT_USER_ID = "current_user_id"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun observeCurrentUserId(): Flow<String?> = callbackFlow {
        // Emit the current value immediately so collectors don't miss the initial state.
        trySend(prefs.getString(KEY_CURRENT_USER_ID, null))

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_CURRENT_USER_ID) {
                trySend(prefs.getString(KEY_CURRENT_USER_ID, null))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    override suspend fun setCurrentUserId(id: String?) {
        withContext(Dispatchers.IO) {
            prefs.edit().apply {
                if (id == null) remove(KEY_CURRENT_USER_ID) else putString(KEY_CURRENT_USER_ID, id)
                apply()
            }
        }
    }

    override suspend fun currentUserIdOnce(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_CURRENT_USER_ID, null)
    }
}
