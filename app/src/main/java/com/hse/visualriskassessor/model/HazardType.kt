package com.hse.visualriskassessor.model

enum class HazardType(val displayName: String, val description: String) {
    SLIP_TRIP_FALL(
        "Slip, Trip & Fall",
        "Potential for slipping, tripping, or falling hazards"
    ),
    ELECTRICAL(
        "Electrical Hazard",
        "Exposed wiring, electrical equipment, or shock risks"
    ),
    CHEMICAL(
        "Chemical Exposure",
        "Presence of hazardous chemicals or substances"
    ),
    FIRE(
        "Fire Hazard",
        "Flammable materials or ignition sources"
    ),
    MACHINERY(
        "Machinery Risk",
        "Moving machinery or equipment without proper guarding"
    ),
    HEIGHT(
        "Working at Height",
        "Elevated work areas or fall protection concerns"
    ),
    ERGONOMIC(
        "Ergonomic Issue",
        "Poor posture, repetitive strain, or manual handling risks"
    ),
    PPE_MISSING(
        "PPE Missing",
        "Required personal protective equipment not in use"
    ),
    STRUCK_BY(
        "Struck By Object",
        "Risk of being hit by moving or falling objects"
    ),
    CONFINED_SPACE(
        "Confined Space",
        "Limited entry/exit or atmospheric hazards"
    ),
    OTHER(
        "Other Hazard",
        "General workplace safety concern"
    );

    companion object {
        fun fromString(value: String): HazardType {
            return values().find { 
                it.name.equals(value, ignoreCase = true) || 
                it.displayName.equals(value, ignoreCase = true) 
            } ?: OTHER
        }
    }
}
