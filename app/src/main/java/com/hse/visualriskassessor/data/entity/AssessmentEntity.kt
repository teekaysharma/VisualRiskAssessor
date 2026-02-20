package com.hse.visualriskassessor.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hse.visualriskassessor.model.Hazard
import com.hse.visualriskassessor.model.RiskLevel
import java.util.Date

@Entity(tableName = "assessments")
data class AssessmentEntity(
    @PrimaryKey
    val id: String,
    val timestamp: Date,
    val imagePath: String,
    val hazards: List<Hazard>,
    val overallRiskLevel: RiskLevel,
    val analysisTimeMs: Long
)
