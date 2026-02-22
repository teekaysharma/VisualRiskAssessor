package com.hse.visualriskassessor.model

import org.junit.Assert.assertTrue
import org.junit.Test

class AssessmentResultTest {

    @Test
    fun fallbackSummary_mentionsFallback() {
        val result = AssessmentResult(
            imagePath = "fake.jpg",
            hazards = listOf(
                Hazard(HazardType.OTHER, likelihood = 2, severity = 2, confidence = 0.5f)
            ),
            overallRiskLevel = RiskLevel.MEDIUM,
            analysisMode = AnalysisMode.HEURISTIC_FALLBACK
        )

        assertTrue(result.usedFallbackAnalysis)
        assertTrue(result.getSummary().contains("fallback", ignoreCase = true))
    }
}
