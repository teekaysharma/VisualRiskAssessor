package com.hse.visualriskassessor.model

import java.util.Date

data class AssessmentResult(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Date = Date(),
    val imagePath: String,
    val hazards: List<Hazard>,
    val overallRiskLevel: RiskLevel,
    val analysisTimeMs: Long = 0
) {
    val hasHazards: Boolean
        get() = hazards.isNotEmpty()

    val highestRiskScore: Int
        get() = hazards.maxOfOrNull { it.riskScore } ?: 0

    fun getAllRecommendations(): List<String> {
        return hazards.flatMap { it.getRecommendations() }.distinct()
    }

    fun getSummary(): String {
        return when {
            !hasHazards -> "No significant hazards detected. Continue regular safety monitoring."
            overallRiskLevel == RiskLevel.LOW -> "Low risk environment detected. Maintain current safety standards."
            overallRiskLevel == RiskLevel.MEDIUM -> "Medium risk hazards identified. Review and implement recommended controls."
            overallRiskLevel == RiskLevel.HIGH -> "High risk hazards present. Immediate action required to implement controls."
            overallRiskLevel == RiskLevel.VERY_HIGH -> "Very high risk environment. Urgent intervention required. Consider stopping work until controls are in place."
            else -> "Extreme risk detected. Stop work immediately and evacuate if necessary. Consult safety professionals before resuming operations."
        }
    }

    fun getHazardsByRiskLevel(): Map<RiskLevel, List<Hazard>> {
        return hazards.groupBy { it.riskLevel }
    }
}
