package com.hse.visualriskassessor.analysis

import com.hse.visualriskassessor.model.Hazard

sealed class HazardDetectionResult {
    data class Success(val hazards: List<Hazard>) : HazardDetectionResult()
    data class Partial(val hazards: List<Hazard>, val warning: String) : HazardDetectionResult()
    data class Error(val exception: Exception, val message: String) : HazardDetectionResult()
    object NoHazardsDetected : HazardDetectionResult()
}
