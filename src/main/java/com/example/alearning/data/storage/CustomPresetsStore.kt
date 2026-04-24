package com.example.alearning.data.storage

import android.content.Context
import com.example.alearning.domain.model.standards.TestPreset
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomPresetsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        const val PREFS_NAME = "alearning_prefs"
        const val KEY_CUSTOM_PRESETS = "custom_test_presets"
    }

    private val gson = Gson()
    private val prefs get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): List<TestPreset> {
        val json = prefs.getString(KEY_CUSTOM_PRESETS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<StoredPreset>>() {}.type
            val stored: List<StoredPreset> = gson.fromJson(json, type) ?: emptyList()
            stored.map { TestPreset(it.id, it.name, null, it.testIds, isBuiltIn = false) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun add(preset: TestPreset) {
        val existing = load().filterNot { it.id == preset.id }
        save(existing + preset.copy(isBuiltIn = false))
    }

    fun delete(presetId: String) {
        save(load().filterNot { it.id == presetId })
    }

    private fun save(presets: List<TestPreset>) {
        val stored = presets.map { StoredPreset(it.id, it.name, it.testIds) }
        prefs.edit().putString(KEY_CUSTOM_PRESETS, gson.toJson(stored)).apply()
    }

    private data class StoredPreset(
        val id: String,
        val name: String,
        val testIds: List<String>
    )
}
