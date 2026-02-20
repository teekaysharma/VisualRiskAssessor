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

    suspend fun analyzeImage(bitmap: Bitmap): HazardDetectionResult {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val hazards = mutableListOf<Hazard>()
        var labelingError: Exception? = null
        var objectError: Exception? = null

        try {
            val labels = imageLabeler.process(inputImage).await()
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
        } catch (e: Exception) {
            labelingError = e
        }

        try {
            val objects = objectDetector.process(inputImage).await()
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
        } catch (e: Exception) {
            objectError = e
        }

        val distinctHazards = hazards.distinctBy { it.type }

        return when {
            labelingError != null && objectError != null -> {
                HazardDetectionResult.Error(
                    exception = labelingError,
                    message = "ML analysis failed: ${labelingError.message}"
                )
            }
            (labelingError != null || objectError != null) && distinctHazards.isNotEmpty() -> {
                val warning = "Partial analysis: one detection model encountered an error"
                HazardDetectionResult.Partial(distinctHazards, warning)
            }
            distinctHazards.isEmpty() -> HazardDetectionResult.NoHazardsDetected
            else -> HazardDetectionResult.Success(distinctHazards)
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

    fun release() {
        imageLabeler.close()
        objectDetector.close()
    }
}
