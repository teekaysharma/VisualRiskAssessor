package com.hse.visualriskassessor.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hse.visualriskassessor.data.dao.AssessmentDao
import com.hse.visualriskassessor.data.entity.AssessmentEntity
import com.hse.visualriskassessor.model.AssessmentResult
import com.hse.visualriskassessor.model.Hazard
import com.hse.visualriskassessor.model.RiskLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AssessmentRepository(private val dao: AssessmentDao) {

    private val gson = Gson()

    val allAssessments: Flow<List<AssessmentResult>> = dao.getAllAssessments().map { entities ->
        entities.map { it.toAssessmentResult() }
    }

    suspend fun saveAssessment(result: AssessmentResult) {
        dao.insertAssessment(result.toEntity())
    }

    suspend fun deleteAssessment(result: AssessmentResult) {
        dao.deleteAssessment(result.toEntity())
    }

    suspend fun getAssessmentById(id: String): AssessmentResult? {
        return dao.getAssessmentById(id)?.toAssessmentResult()
    }

    private fun AssessmentResult.toEntity(): AssessmentEntity {
        return AssessmentEntity(
            id = id,
            timestamp = timestamp.time,
            imagePath = imagePath,
            hazardsJson = gson.toJson(hazards),
            overallRiskLevel = overallRiskLevel.name,
            analysisTimeMs = analysisTimeMs
        )
    }

    private fun AssessmentEntity.toAssessmentResult(): AssessmentResult {
        val hazardType = object : TypeToken<List<Hazard>>() {}.type
        val hazards: List<Hazard> = try {
            gson.fromJson(hazardsJson, hazardType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        return AssessmentResult(
            id = id,
            timestamp = java.util.Date(timestamp),
            imagePath = imagePath,
            hazards = hazards,
            overallRiskLevel = RiskLevel.valueOf(overallRiskLevel),
            analysisTimeMs = analysisTimeMs
        )
    }
}
