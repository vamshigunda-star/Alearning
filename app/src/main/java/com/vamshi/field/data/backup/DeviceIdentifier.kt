package com.vamshi.field.data.backup

import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Identifies this install for per-device Google Drive backups.
 *
 * [deviceId] is a random UUID generated once and persisted in `app_prefs` — it survives
 * app restarts but not a reinstall/uninstall, which is fine here: it only needs to keep
 * this device's own backups from colliding with a *different* device's, not to be a
 * durable hardware identifier.
 */
@Singleton
class DeviceIdentifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    val deviceId: String by lazy {
        prefs.getString(KEY_DEVICE_ID, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_DEVICE_ID, it).apply()
        }
    }

    val deviceLabel: String
        get() = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifBlank { "Unknown device" }

    /** Stable per-device Drive filename: label kept for readability, id suffix guarantees uniqueness. */
    val backupFileName: String
        get() = "field_backup_${sanitize(deviceLabel)}_${deviceId.take(8)}.json"

    private fun sanitize(label: String) = label.replace(Regex("[^A-Za-z0-9]+"), "-").trim('-')

    private companion object {
        const val KEY_DEVICE_ID = "device_id"
    }
}
