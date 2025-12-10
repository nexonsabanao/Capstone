package com.example.nutriority.data.local

import androidx.room.TypeConverter

class Converters {
    /**
     * Converts a comma-separated String from the database into a List of Strings.
     */
    @TypeConverter
    fun fromString(value: String): List<String> {
        // If the stored string is empty, return an empty list to avoid issues.
        return if (value.isEmpty()) {
            emptyList()
        } else {
            value.split(",").map { it.trim() }
        }
    }

    /**
     * Converts a List of Strings into a single comma-separated String to store in the database.
     */
    @TypeConverter
    fun fromList(list: List<String>): String {
        return list.joinToString(",")
    }
}