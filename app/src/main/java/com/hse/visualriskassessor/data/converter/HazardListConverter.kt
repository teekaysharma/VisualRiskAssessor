package com.hse.visualriskassessor.data.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hse.visualriskassessor.model.Hazard

class HazardListConverter {

    private val gson = Gson()

    @TypeConverter
    fun fromHazardList(hazards: List<Hazard>?): String? {
        return hazards?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toHazardList(value: String?): List<Hazard> {
        if (value.isNullOrBlank()) {
            return emptyList()
        }
        val type = object : TypeToken<List<Hazard>>() {}.type
        return gson.fromJson(value, type)
    }
}
