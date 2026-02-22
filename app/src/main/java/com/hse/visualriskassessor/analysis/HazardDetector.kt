package com.hse.visualriskassessor.analysis

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.hse.visualriskassessor.model.AnalysisMode
import com.hse.visualriskassessor.model.Hazard
import com.hse.visualriskassessor.model.HazardType
import kotlinx.coroutines.tasks.await

data class HazardDetectionResult(
    val hazards: List<Hazard>,
    val mode: AnalysisMode
)

class HazardDetector {

    enum class AnalysisStatus {
        SUCCESS,
        PARTIAL,
        FALLBACK
    }

    data class HazardAnalysisResult(
        val hazards: List<Hazard>,
        val status: AnalysisStatus
    )

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

    suspend fun analyzeImage(bitmap: Bitmap): HazardDetectionResult {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val hazards = mutableListOf<Hazard>()
        var usedFallback = false
        var partialFailure = false

        try {
            val labels = imageLabeler.process(inputImage).await()

            val objects = try {
                objectDetector.process(inputImage).await()
            } catch (exception: Exception) {
                partialFailure = true
                emptyList()
            }

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
                return HazardDetectionResult(
                    hazards = generateSampleHazards(bitmap).distinctBy { it.type },
                    mode = AnalysisMode.HEURISTIC_FALLBACK
                )
            }

            return HazardDetectionResult(
                hazards = hazards.distinctBy { it.type },
                mode = AnalysisMode.ML_DETECTION
            )
        } catch (_: Exception) {
            return HazardDetectionResult(
                hazards = generateSampleHazards(bitmap).distinctBy { it.type },
                mode = AnalysisMode.HEURISTIC_FALLBACK
            )
        }
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
                lowerLabel.contains("glove") || lowerLabel.contains("safety") -> if (confidence < 0.7f) HazardType.PPE_MISSING else null
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
                    confidence = 0.65f,
                    details = "Low visibility conditions may increase slip/trip hazards"
                )
            )
        }

        if (metrics.edgeDensity > 0.15f) {
            hazards.add(
                Hazard(
                    type = HazardType.MACHINERY,
                    likelihood = 2,
                    severity = 3,
                    confidence = 0.55f,
                    details = "Complex visual environment suggests machinery/equipment presence"
                )
            )
        }

        if (metrics.colorVariance > 40f) {
            hazards.add(
                Hazard(
                    type = HazardType.OTHER,
                    likelihood = 2,
                    severity = 2,
                    confidence = 0.5f,
                    details = "General workplace assessment - manual verification recommended"
                )
            )
        }

        if (hazards.isEmpty()) {
            hazards.add(
                Hazard(
                    type = HazardType.OTHER,
                    likelihood = 1,
                    severity = 2,
                    confidence = 0.4f,
                    details = "No specific hazards automatically detected - fallback estimation used"
                )
            )
        }

        return hazards
    }

    private data class ImageMetrics(
        val averageBrightness: Int,
        val edgeDensity: Float,
        val colorVariance: Float
    )

    private fun calculateImageMetrics(bitmap: Bitmap): ImageMetrics {
        val width = bitmap.width
        val height = bitmap.height
        val sampleSize = 10

        var totalBrightness = 0L
        var pixelCount = 0
        var edgeCount = 0

        for (x in 0 until width step sampleSize) {
            for (y in 0 until height step sampleSize) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val brightness = (r + g + b) / 3
                totalBrightness += brightness
                pixelCount++

                if (x > 0 && y > 0) {
                    val prevPixel = bitmap.getPixel(x - sampleSize.coerceAtMost(x), y)
                    val prevR = (prevPixel shr 16) and 0xFF
                    val diff = kotlin.math.abs(r - prevR)
                    if (diff > 30) edgeCount++
                }
            }
        }

        val averageBrightness = if (pixelCount > 0) (totalBrightness / pixelCount).toInt() else 128
        val edgeDensity = if (pixelCount > 0) edgeCount.toFloat() / pixelCount else 0f
        val colorVariance = calculateColorVariance(bitmap, sampleSize)

        return ImageMetrics(averageBrightness, edgeDensity, colorVariance)
    }

    private fun calculateColorVariance(bitmap: Bitmap, sampleSize: Int): Float {
        val colors = mutableListOf<Int>()
        for (x in 0 until bitmap.width step sampleSize) {
            for (y in 0 until bitmap.height step sampleSize) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                colors.add((r + g + b) / 3)
            }
        }
        if (colors.isEmpty()) return 0f
        val mean = colors.average()
        return kotlin.math.sqrt(colors.map { (it - mean) * (it - mean) }.average()).toFloat()
    }

    fun release() {
        imageLabeler.close()
        objectDetector.close()
    }
}
