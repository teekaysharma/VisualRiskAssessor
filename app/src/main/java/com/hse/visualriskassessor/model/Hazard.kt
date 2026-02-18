package com.hse.visualriskassessor.model

data class Hazard(
    val type: HazardType,
    val likelihood: Int,
    val severity: Int,
    val confidence: Float,
    val location: String? = null,
    val details: String? = null
) {
    val riskLevel: RiskLevel
        get() = RiskLevel.calculate(likelihood, severity)

    val riskScore: Int
        get() = likelihood * severity

    fun getRecommendations(): List<String> {
        return when (type) {
            HazardType.SLIP_TRIP_FALL -> listOf(
                "Clear walkways of obstacles and debris",
                "Ensure proper lighting in all areas",
                "Install non-slip surfaces or mats",
                "Mark wet or slippery areas with warning signs",
                "Implement regular housekeeping inspections"
            )
            HazardType.ELECTRICAL -> listOf(
                "Isolate and lockout exposed electrical equipment",
                "Ensure all electrical work is performed by qualified personnel",
                "Implement regular electrical safety inspections",
                "Provide appropriate PPE (insulated gloves, tools)",
                "Install warning signs near electrical hazards"
            )
            HazardType.CHEMICAL -> listOf(
                "Ensure proper storage and labeling of chemicals",
                "Provide appropriate PPE (gloves, goggles, respirators)",
                "Maintain Safety Data Sheets (SDS) accessibility",
                "Implement proper ventilation systems",
                "Train workers on chemical handling procedures"
            )
            HazardType.FIRE -> listOf(
                "Remove or properly store flammable materials",
                "Ensure fire extinguishers are accessible and maintained",
                "Install and test smoke detectors regularly",
                "Create and practice emergency evacuation plans",
                "Eliminate ignition sources in hazardous areas"
            )
            HazardType.MACHINERY -> listOf(
                "Install proper machine guarding",
                "Implement lockout/tagout procedures",
                "Provide comprehensive operator training",
                "Conduct regular machinery maintenance",
                "Install emergency stop buttons"
            )
            HazardType.HEIGHT -> listOf(
                "Install guardrails and fall protection systems",
                "Ensure workers use appropriate fall arrest equipment",
                "Provide training on working at height procedures",
                "Inspect all fall protection equipment regularly",
                "Use scaffolding or elevated work platforms properly"
            )
            HazardType.ERGONOMIC -> listOf(
                "Adjust workstations to proper ergonomic height",
                "Provide mechanical aids for lifting and handling",
                "Implement job rotation to reduce repetitive strain",
                "Train workers on proper lifting techniques",
                "Conduct ergonomic assessments regularly"
            )
            HazardType.PPE_MISSING -> listOf(
                "Ensure appropriate PPE is provided and available",
                "Conduct PPE training and fit testing",
                "Implement PPE inspection and maintenance programs",
                "Enforce PPE usage policies consistently",
                "Assess workplace to determine required PPE"
            )
            HazardType.STRUCK_BY -> listOf(
                "Establish exclusion zones around moving equipment",
                "Install protective barriers or screens",
                "Implement traffic management plans",
                "Use high-visibility clothing in active areas",
                "Secure materials to prevent falling objects"
            )
            HazardType.CONFINED_SPACE -> listOf(
                "Implement confined space entry procedures",
                "Test atmosphere before and during entry",
                "Provide adequate ventilation",
                "Ensure rescue equipment is readily available",
                "Assign trained attendants for confined space work"
            )
            HazardType.OTHER -> listOf(
                "Conduct detailed risk assessment",
                "Consult with safety professionals",
                "Implement appropriate control measures",
                "Train workers on identified risks",
                "Monitor and review controls regularly"
            )
        }
    }
}
