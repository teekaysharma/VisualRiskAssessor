package com.hse.visualriskassessor.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hse.visualriskassessor.data.entity.AssessmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentDao {

    @Query("SELECT * FROM assessments ORDER BY timestamp DESC")
    fun getAllAssessments(): Flow<List<AssessmentEntity>>

    @Query("SELECT * FROM assessments WHERE id = :id")
    suspend fun getAssessmentById(id: String): AssessmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessment(assessment: AssessmentEntity)

    @Delete
    suspend fun deleteAssessment(assessment: AssessmentEntity)

    @Query("DELETE FROM assessments")
    suspend fun deleteAllAssessments()
}
