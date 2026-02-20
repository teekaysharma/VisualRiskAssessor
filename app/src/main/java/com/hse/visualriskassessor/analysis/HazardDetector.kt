package com.hse.visualriskassessor.analysis

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.hse.visualriskassessor.model.Hazard
import com.hse.visualriskassessor.model.HazardType
import kotlinx.coroutines.tasks.await

class HazardDetector(private val context: Context) {

    private val imageLabeler by lazy {
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.5f)
            .build()
        ImageLabeling.getClient(options)
    }

    private val objectDetector by lazy {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        ObjectDetection.getClient(options)
    }

    suspend fun analyzeImage(bitmap: Bitmap): List<Hazard> {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val hazards = mutableListOf<Hazard>()

        try {
            val labels = imageLabeler.process(inputImage).await()
            val objects = objectDetector.process(inputImage).await()

            for (label in labels) {
                val hazardType = mapLabelToHazard(label.text, label.confidence)
                if (hazardType != null) {
                    val (likelihood, severity) = estimateRiskFactors(label.text, label.confidence)
                    hazards.add(
                        Hazard(
                            type = hazardType,
                            likelihood = likelihood,
                            severity = severity,
                            confidence = label.confidence,
                            details = "Detected: ${label.text} (${(label.confidence * 100).toInt()}% confidence)"
                        )
                    )
                }
            }

            for (obj in objects) {
                obj.labels.forEach { label ->
                    val hazardType = mapObjectToHazard(label.text, label.confidence)
                    if (hazardType != null && !hazards.any { it.type == hazardType }) {
                        val (likelihood, severity) = estimateRiskFactors(label.text, label.confidence)
                        hazards.add(
                            Hazard(
                                type = hazardType,
                                likelihood = likelihood,
                                severity = severity,
                                confidence = label.confidence ?: 0f,
                                location = "Object detected in image",
                                details = label.text
                            )
                        )
                    }
                }
            }

            if (hazards.isEmpty()) {
                hazards.addAll(generateSampleHazards(bitmap))
            }

        } catch (e: Exception) {
            hazards.addAll(generateSampleHazards(bitmap))
        }

        return hazards.distinctBy { it.type }
    }

    private fun mapLabelToHazard(label: String, confidence: Float): HazardType? {
        val lowerLabel = label.lowercase()
        return when {
            lowerLabel.contains("wire") || lowerLabel.contains("cable") || 
            lowerLabel.contains("electric") || lowerLabel.contains("plug") -> HazardType.ELECTRICAL
            
            lowerLabel.contains("ladder") || lowerLabel.contains("height") || 
            lowerLabel.contains("scaffold") || lowerLabel.contains("roof") -> HazardType.HEIGHT
            
            lowerLabel.contains("machine") || lowerLabel.contains("equipment") || 
            lowerLabel.contains("tool") || lowerLabel.contains("motor") -> HazardType.MACHINERY
            
            lowerLabel.contains("chemical") || lowerLabel.contains("bottle") || 
            lowerLabel.contains("container") || lowerLabel.contains("barrel") -> HazardType.CHEMICAL
            
            lowerLabel.contains("fire") || lowerLabel.contains("flame") || 
            lowerLabel.contains("gas") || lowerLabel.contains("flammable") -> HazardType.FIRE
            
            lowerLabel.contains("floor") || lowerLabel.contains("ground") || 
            lowerLabel.contains("wet") || lowerLabel.contains("slippery") -> HazardType.SLIP_TRIP_FALL
            
            lowerLabel.contains("hard hat") || lowerLabel.contains("helmet") || 
            lowerLabel.contains("glove") || lowerLabel.contains("safety") -> 
                if (confidence < 0.7f) HazardType.PPE_MISSING else null
            
            else -> null
        }
    }

    private fun mapObjectToHazard(objectLabel: String, confidence: Float?): HazardType? {
        return mapLabelToHazard(objectLabel, confidence ?: 0f)
    }

    private fun estimateRiskFactors(label: String, confidence: Float): Pair<Int, Int> {
        val baseLikelihood = when {
            confidence > 0.8f -> 4
            confidence > 0.6f -> 3
            else -> 2
        }

        val baseSeverity = when (label.lowercase()) {
            "electrical", "fire", "height", "chemical" -> 4
            "machinery", "confined space" -> 3
            else -> 2
        }

        return Pair(baseLikelihood, baseSeverity)
    }

    private fun generateSampleHazards(bitmap: Bitmap): List<Hazard> {
        val metrics = calculateImageMetrics(bitmap)
        val hazards = mutableListOf<Hazard>()

        if (metrics.averageBrightness < 90) {
            hazards.add(
                Hazard(
                    type = HazardType.SLIP_TRIP_FALL,
                    likelihood = 3,
                    severity = 3,
                    confidence = 0.72f,
                    details = "Low lighting detected - increased risk of slips, trips, and falls"
                )
            )
        }

        if (metrics.redDominance > 25) {
            hazards.add(
                Hazard(
                    type = HazardType.FIRE,
                    likelihood = 2,
                    severity = 4,
                    confidence = 0.65f,
                    details = "Warm color dominance detected - verify presence of heat or flammable sources"
                )
            )
        }

        if (metrics.contrast > 60) {
            hazards.add(
                Hazard(
                    type = HazardType.MACHINERY,
                    likelihood = 2,
                    severity = 3,
                    confidence = 0.6f,
                    details = "High contrast scene suggests equipment or moving parts - review guarding and controls"
                )
            )
        }

        val imageComplexity = bitmap.width * bitmap.height
        if (imageComplexity > 1000000) {
            hazards.add(
                Hazard(
                    type = HazardType.OTHER,
                    likelihood = 2,
                    severity = 2,
                    confidence = 0.62f,
                    details = "Complex work environment - comprehensive assessment recommended"
                )
            )
        }

        if (hazards.isEmpty()) {
            hazards.add(
                Hazard(
                    type = HazardType.OTHER,
                    likelihood = 2,
                    severity = 2,
                    confidence = 0.6f,
                    details = "No specific hazards detected - continue routine safety monitoring"
                )
            )
        }

        return hazards
    }

    private data class ImageMetrics(
        val averageBrightness: Int,
        val contrast: Double,
        val redDominance: Double
    )

    private fun calculateImageMetrics(bitmap: Bitmap): ImageMetrics {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var totalBrightness = 0L
        var totalRed = 0L
        var totalGreen = 0L
        var totalBlue = 0L

        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xff
            val g = (pixel shr 8) and 0xff
            val b = pixel and 0xff
            totalRed += r
            totalGreen += g
            totalBlue += b
            totalBrightness += (r + g + b) / 3
        }

        val averageBrightness = (totalBrightness / pixels.size).toInt()
        val averageRed = totalRed.toDouble() / pixels.size
        val averageGreen = totalGreen.toDouble() / pixels.size
        val averageBlue = totalBlue.toDouble() / pixels.size

        var varianceSum = 0.0
        for (pixel in pixels) {
            val r = (pixel shr 16) and 0xff
            val g = (pixel shr 8) and 0xff
            val b = pixel and 0xff
            val brightness = (r + g + b) / 3.0
            val diff = brightness - averageBrightness
            varianceSum += diff * diff
        }

        val contrast = kotlin.math.sqrt(varianceSum / pixels.size)
        val redDominance = averageRed - ((averageGreen + averageBlue) / 2.0)

        return ImageMetrics(
            averageBrightness = averageBrightness,
            contrast = contrast,
            redDominance = redDominance
        )
    }

    fun release() {
        imageLabeler.close()
        objectDetector.close()
    }
}
