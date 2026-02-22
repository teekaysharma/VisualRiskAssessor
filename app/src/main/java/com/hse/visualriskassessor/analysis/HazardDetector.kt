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

        try {
            val labels = imageLabeler.process(inputImage).await()

            val objects = try {
                objectDetector.process(inputImage).await()
            } catch (_: Exception) {
                emptyList()
            }

            for (label in labels) {
                val hazardType = mapLabelToHazard(label.text, label.confidence)
                if (hazardType != null) {
                    val (likelihood, severity) = estimateRiskFactors(hazardType, label.confidence)
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
                    val hazardType = mapLabelToHazard(label.text, label.confidence ?: 0f)
                    if (hazardType != null && !hazards.any { it.type == hazardType }) {
                        val (likelihood, severity) = estimateRiskFactors(hazardType, label.confidence ?: 0f)
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
                    hazards = generateHeuristicHazards(bitmap).distinctBy { it.type },
                    mode = AnalysisMode.HEURISTIC_FALLBACK
                )
            }

            return HazardDetectionResult(
                hazards = hazards.distinctBy { it.type },
                mode = AnalysisMode.ML_DETECTION
            )
        } catch (_: Exception) {
            return HazardDetectionResult(
                hazards = generateHeuristicHazards(bitmap).distinctBy { it.type },
                mode = AnalysisMode.HEURISTIC_FALLBACK
            )
        }
    }

    private fun mapLabelToHazard(label: String, confidence: Float): HazardType? {
        val l = label.lowercase()
        return when {
            // Electrical
            l.contains("wire") || l.contains("cable") || l.contains("electric") ||
                l.contains("plug") || l.contains("outlet") || l.contains("circuit") ||
                l.contains("socket") || l.contains("fuse") || l.contains("switchboard") ||
                l.contains("pylon") || l.contains("transformer") -> HazardType.ELECTRICAL

            // Fire
            l.contains("fire") || l.contains("flame") || l.contains("flammable") ||
                l.contains("smoke") || l.contains("combustible") || l.contains("ignition") ||
                l.contains("gas cylinder") || l.contains("propane") || l.contains("acetylene") ||
                l.contains("welding") || l.contains("torch") -> HazardType.FIRE

            // Height
            l.contains("ladder") || l.contains("scaffold") || l.contains("roof") ||
                l.contains("height") || l.contains("elevated") || l.contains("platform") ||
                l.contains("balcony") || l.contains("catwalk") || l.contains("mast") ||
                l.contains("tower") || l.contains("stair") || l.contains("step") -> HazardType.HEIGHT

            // Machinery
            l.contains("machine") || l.contains("equipment") || l.contains("motor") ||
                l.contains("drill") || l.contains("lathe") || l.contains("grinder") ||
                l.contains("conveyor") || l.contains("press") || l.contains("saw") ||
                l.contains("compressor") || l.contains("pump") || l.contains("turbine") ||
                l.contains("generator") || l.contains("engine") -> HazardType.MACHINERY

            // Chemical
            l.contains("chemical") || l.contains("barrel") || l.contains("drum") ||
                l.contains("canister") || l.contains("tank") || l.contains("container") ||
                l.contains("bottle") || l.contains("hazmat") || l.contains("corrosive") ||
                l.contains("toxic") || l.contains("solvent") || l.contains("acid") ||
                l.contains("reagent") || l.contains("biohazard") -> HazardType.CHEMICAL

            // Slip, Trip, Fall
            l.contains("floor") || l.contains("ground") || l.contains("wet") ||
                l.contains("slippery") || l.contains("puddle") || l.contains("spill") ||
                l.contains("uneven") || l.contains("debris") || l.contains("clutter") ||
                l.contains("obstruction") || l.contains("hose") -> HazardType.SLIP_TRIP_FALL

            // Struck By
            l.contains("crane") || l.contains("forklift") || l.contains("pallet") ||
                l.contains("load") || l.contains("overhead") || l.contains("suspended") ||
                l.contains("falling object") || l.contains("projectile") ||
                l.contains("vehicle") || l.contains("truck") || l.contains("excavator") -> HazardType.STRUCK_BY

            // Confined Space
            l.contains("pipe") || l.contains("tunnel") || l.contains("manhole") ||
                l.contains("shaft") || l.contains("silo") || l.contains("vault") ||
                l.contains("trench") || l.contains("duct") || l.contains("confined") ||
                l.contains("underground") -> HazardType.CONFINED_SPACE

            // Ergonomic
            l.contains("posture") || l.contains("keyboard") || l.contains("monitor") ||
                l.contains("repetitive") || l.contains("ergonomic") || l.contains("lifting") ||
                l.contains("awkward") || l.contains("manual handling") ||
                l.contains("workstation") || l.contains("desk") -> HazardType.ERGONOMIC

            // PPE Missing — flag when PPE item seen but confidence low (likely absent/partial)
            l.contains("hard hat") || l.contains("helmet") || l.contains("safety vest") ||
                l.contains("high visibility") || l.contains("glove") ||
                l.contains("respirator") || l.contains("goggles") ||
                l.contains("safety boot") || l.contains("harness") ->
                if (confidence < 0.72f) HazardType.PPE_MISSING else null

            else -> null
        }
    }

    private fun estimateRiskFactors(hazardType: HazardType, confidence: Float): Pair<Int, Int> {
        val likelihood = when {
            confidence > 0.85f -> 4
            confidence > 0.65f -> 3
            else -> 2
        }

        val severity = when (hazardType) {
            HazardType.FIRE -> 5
            HazardType.ELECTRICAL -> 4
            HazardType.HEIGHT -> 4
            HazardType.CHEMICAL -> 4
            HazardType.CONFINED_SPACE -> 4
            HazardType.STRUCK_BY -> 4
            HazardType.MACHINERY -> 3
            HazardType.PPE_MISSING -> 3
            HazardType.SLIP_TRIP_FALL -> 3
            HazardType.ERGONOMIC -> 2
            HazardType.OTHER -> 2
        }

        return Pair(likelihood, severity)
    }

    private fun generateHeuristicHazards(bitmap: Bitmap): List<Hazard> {
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
                    details = "Complex visual environment suggests machinery or equipment presence"
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
                    details = "General workplace assessment — manual verification recommended"
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
                    details = "No specific hazards automatically detected — fallback estimation used"
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
