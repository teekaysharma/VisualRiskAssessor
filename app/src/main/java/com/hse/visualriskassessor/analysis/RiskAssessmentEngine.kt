package com.hse.visualriskassessor.analysis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.hse.visualriskassessor.model.AssessmentResult
import com.hse.visualriskassessor.model.Hazard
import com.hse.visualriskassessor.model.RiskLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class RiskAssessmentEngine(private val context: Context) {

    private val hazardDetector = HazardDetector()

    suspend fun assessImage(uri: Uri): AssessmentResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        val bitmap = loadBitmap(uri)
        val processedBitmap = preprocessImage(bitmap)

        val savedPath = saveImage(processedBitmap)

        val detectionResult = hazardDetector.analyzeImage(processedBitmap)
        val hazards = detectionResult.hazards
        val overallRisk = calculateOverallRisk(hazards)

        val analysisTime = System.currentTimeMillis() - startTime

        AssessmentResult(
            imagePath = savedPath,
            hazards = hazards,
            overallRiskLevel = overallRisk,
            analysisTimeMs = analysisTime,
            analysisMode = detectionResult.mode
        )
    }

    suspend fun assessImage(bitmap: Bitmap): AssessmentResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        val processedBitmap = preprocessImage(bitmap)
        val savedPath = saveImage(processedBitmap)

        val detectionResult = hazardDetector.analyzeImage(processedBitmap)
        val hazards = detectionResult.hazards
        val overallRisk = calculateOverallRisk(hazards)

        val analysisTime = System.currentTimeMillis() - startTime

        AssessmentResult(
            imagePath = savedPath,
            hazards = hazards,
            overallRiskLevel = overallRisk,
            analysisTimeMs = analysisTime,
            analysisMode = detectionResult.mode
        )
    }

    private fun loadBitmap(uri: Uri): Bitmap {
        val inputStream = context.contentResolver.openInputStream(uri)
        return BitmapFactory.decodeStream(inputStream).also {
            inputStream?.close()
        }
    }

    private fun preprocessImage(bitmap: Bitmap): Bitmap {
        val maxSize = 1024
        val scale = minOf(
            maxSize.toFloat() / bitmap.width,
            maxSize.toFloat() / bitmap.height,
            1f
        )

        if (scale < 1f) {
            val newWidth = (bitmap.width * scale).toInt()
            val newHeight = (bitmap.height * scale).toInt()
            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }

        return bitmap
    }

    private fun saveImage(bitmap: Bitmap): String {
        val imagesDir = File(context.getExternalFilesDir(null), "images")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }

        val timestamp = System.currentTimeMillis()
        val imageFile = File(imagesDir, "assessment_$timestamp.jpg")

        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        return imageFile.absolutePath
    }

    private fun calculateOverallRisk(hazards: List<Hazard>): RiskLevel {
        if (hazards.isEmpty()) {
            return RiskLevel.LOW
        }

        val maxRiskScore = hazards.maxOf { it.riskScore }
        val avgRiskScore = hazards.map { it.riskScore }.average().toInt()

        val extremeCount = hazards.count { it.riskLevel == RiskLevel.EXTREME }
        val veryHighCount = hazards.count { it.riskLevel == RiskLevel.VERY_HIGH }
        val highCount = hazards.count { it.riskLevel == RiskLevel.HIGH }

        return when {
            extremeCount > 0 || maxRiskScore >= 20 -> RiskLevel.EXTREME
            veryHighCount >= 2 || maxRiskScore >= 16 -> RiskLevel.VERY_HIGH
            veryHighCount >= 1 || highCount >= 2 || maxRiskScore >= 10 -> RiskLevel.HIGH
            highCount >= 1 || avgRiskScore >= 6 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }

    fun release() {
        hazardDetector.release()
    }
}
