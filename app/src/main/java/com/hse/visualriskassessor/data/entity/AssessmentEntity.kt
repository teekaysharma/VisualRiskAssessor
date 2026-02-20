package com.hse.visualriskassessor.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assessments")
data class AssessmentEntity(
    @PrimaryKey
    val id: String,
    val timestamp: Long,
    val imagePath: String,
    val hazardsJson: String,
    val overallRiskLevel: String,
    val analysisTimeMs: Long
)
