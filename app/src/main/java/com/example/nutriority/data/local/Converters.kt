package com.example.nutriority.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {
    /**
     * Converts a JSON String from the database into a List of Strings.
     */
    @TypeConverter
    fun fromString(value: String?): List<String> {
        if (value == null) {
            return emptyList()
        }
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    /**
     * Converts a List of Strings into a single JSON String to store in the database.
     */
    @TypeConverter
    fun fromList(list: List<String>?): String {
        if (list == null) {
            return "[]"
        }
        val gson = Gson()
        return gson.toJson(list)
    }

    /**
     * Converts a Long timestamp from the database into a Date object.
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    /**
     * Converts a Date object into a Long timestamp to store in the database.
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}