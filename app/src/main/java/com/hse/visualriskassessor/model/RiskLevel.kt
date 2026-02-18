package com.hse.visualriskassessor.model

enum class RiskLevel(val displayName: String, val colorRes: Int, val score: Int) {
    LOW("Low Risk", android.R.color.holo_green_dark, 1),
    MEDIUM("Medium Risk", android.R.color.holo_orange_light, 2),
    HIGH("High Risk", android.R.color.holo_orange_dark, 3),
    VERY_HIGH("Very High Risk", android.R.color.holo_red_light, 4),
    EXTREME("Extreme Risk", android.R.color.holo_red_dark, 5);

    companion object {
        fun fromScore(score: Int): RiskLevel {
            return when (score) {
                in 1..4 -> LOW
                in 5..9 -> MEDIUM
                in 10..15 -> HIGH
                in 16..20 -> VERY_HIGH
                else -> EXTREME
            }
        }

        fun calculate(likelihood: Int, severity: Int): RiskLevel {
            val score = likelihood * severity
            return fromScore(score)
        }
    }
}
