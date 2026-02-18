package com.hse.visualriskassessor.model

import org.junit.Assert.*
import org.junit.Test

class RiskLevelTest {

    @Test
    fun testRiskLevelFromScore_Low() {
        assertEquals(RiskLevel.LOW, RiskLevel.fromScore(1))
        assertEquals(RiskLevel.LOW, RiskLevel.fromScore(4))
    }

    @Test
    fun testRiskLevelFromScore_Medium() {
        assertEquals(RiskLevel.MEDIUM, RiskLevel.fromScore(5))
        assertEquals(RiskLevel.MEDIUM, RiskLevel.fromScore(9))
    }

    @Test
    fun testRiskLevelFromScore_High() {
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(10))
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(15))
    }

    @Test
    fun testRiskLevelFromScore_VeryHigh() {
        assertEquals(RiskLevel.VERY_HIGH, RiskLevel.fromScore(16))
        assertEquals(RiskLevel.VERY_HIGH, RiskLevel.fromScore(20))
    }

    @Test
    fun testRiskLevelFromScore_Extreme() {
        assertEquals(RiskLevel.EXTREME, RiskLevel.fromScore(21))
        assertEquals(RiskLevel.EXTREME, RiskLevel.fromScore(25))
    }

    @Test
    fun testCalculateRiskLevel() {
        assertEquals(RiskLevel.LOW, RiskLevel.calculate(1, 1))
        assertEquals(RiskLevel.MEDIUM, RiskLevel.calculate(2, 3))
        assertEquals(RiskLevel.HIGH, RiskLevel.calculate(3, 4))
        assertEquals(RiskLevel.VERY_HIGH, RiskLevel.calculate(4, 4))
        assertEquals(RiskLevel.EXTREME, RiskLevel.calculate(5, 5))
    }

    @Test
    fun testRiskLevelProperties() {
        assertEquals("Low Risk", RiskLevel.LOW.displayName)
        assertEquals(1, RiskLevel.LOW.score)
        
        assertEquals("Medium Risk", RiskLevel.MEDIUM.displayName)
        assertEquals(2, RiskLevel.MEDIUM.score)
        
        assertEquals("High Risk", RiskLevel.HIGH.displayName)
        assertEquals(3, RiskLevel.HIGH.score)
    }
}
