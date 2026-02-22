package com.hse.visualriskassessor.data.repository

import com.hse.visualriskassessor.data.dao.AssessmentDao
import com.hse.visualriskassessor.data.entity.AssessmentEntity
import com.hse.visualriskassessor.model.AssessmentResult
import com.hse.visualriskassessor.model.OperationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

class AssessmentRepository(private val assessmentDao: AssessmentDao) {

    fun getAssessments(): Flow<List<AssessmentResult>> {
        return assessmentDao.getAssessments().map { assessments ->
            assessments.map { it.toModel() }
        }
    }

    suspend fun saveAssessment(result: AssessmentResult): OperationResult<Unit> {
        return try {
            assessmentDao.insertAssessment(result.toEntity())
            OperationResult.Success(Unit)
        } catch (exception: Exception) {
            OperationResult.Error("Unable to save assessment.", exception)
        }
    }

    suspend fun deleteAssessment(result: AssessmentResult): OperationResult<Unit> {
        return try {
            assessmentDao.deleteAssessmentById(result.id)
            deleteImageIfExists(result.imagePath)
            OperationResult.Success(Unit)
        } catch (exception: Exception) {
            OperationResult.Error("Unable to delete assessment.", exception)
        }
    }

    suspend fun clearAssessments(results: List<AssessmentResult>): OperationResult<Unit> {
        return try {
            assessmentDao.clearAssessments()
            results.forEach { deleteImageIfExists(it.imagePath) }
            OperationResult.Success(Unit)
        } catch (exception: Exception) {
            OperationResult.Error("Unable to clear assessments.", exception)
        }
    }

    private fun deleteImageIfExists(path: String) {
        kotlin.runCatching {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    private fun AssessmentEntity.toModel(): AssessmentResult {
        return AssessmentResult(
            id = id,
            timestamp = timestamp,
            imagePath = imagePath,
            hazards = hazards,
            overallRiskLevel = overallRiskLevel,
            analysisTimeMs = analysisTimeMs,
            analysisMode = analysisMode
        )
    }

    private fun AssessmentResult.toEntity(): AssessmentEntity {
        return AssessmentEntity(
            id = id,
            timestamp = timestamp,
            imagePath = imagePath,
            hazards = hazards,
            overallRiskLevel = overallRiskLevel,
            analysisTimeMs = analysisTimeMs,
            analysisMode = analysisMode
        )
    }
}
