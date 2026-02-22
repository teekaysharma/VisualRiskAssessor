package com.hse.visualriskassessor.data.converter

import androidx.room.TypeConverter
import com.hse.visualriskassessor.model.AnalysisMode

class AnalysisModeConverter {
    @TypeConverter
    fun fromAnalysisMode(mode: AnalysisMode?): String? = mode?.name

    @TypeConverter
    fun toAnalysisMode(value: String?): AnalysisMode =
        value?.let { AnalysisMode.valueOf(it) } ?: AnalysisMode.ML_DETECTION
}
