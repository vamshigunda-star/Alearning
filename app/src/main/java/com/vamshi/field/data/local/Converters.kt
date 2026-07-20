package com.vamshi.field.data.local

import androidx.room.TypeConverter
import com.vamshi.field.domain.model.people.BiologicalSex

object Converters {
    @TypeConverter
    @JvmStatic
    fun fromBiologicalSex(value: BiologicalSex): String = value.name

    @TypeConverter
    @JvmStatic
    fun toBiologicalSex(value: String): BiologicalSex {
        return try {
            BiologicalSex.valueOf(value)
        } catch (e: Exception) {
            BiologicalSex.UNSPECIFIED
        }
    }
}