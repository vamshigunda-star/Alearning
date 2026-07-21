package com.vamshi.field.data.local

import androidx.room3.ColumnTypeConverter
import com.vamshi.field.domain.model.people.BiologicalSex

object Converters {
    @ColumnTypeConverter
    @JvmStatic
    fun fromBiologicalSex(value: BiologicalSex): String = value.name

    @ColumnTypeConverter
    @JvmStatic
    fun toBiologicalSex(value: String): BiologicalSex {
        return try {
            BiologicalSex.valueOf(value)
        } catch (e: Exception) {
            BiologicalSex.UNSPECIFIED
        }
    }
}
