package com.hse.visualriskassessor.data.converter

import androidx.room.TypeConverter
import com.hse.visualriskassessor.model.RiskLevel

class RiskLevelConverter {

    @TypeConverter
    fun fromRiskLevel(riskLevel: RiskLevel?): String? {
        return riskLevel?.name
    }

    @TypeConverter
    fun toRiskLevel(value: String?): RiskLevel? {
        return value?.let { RiskLevel.valueOf(it) }
    }
}
