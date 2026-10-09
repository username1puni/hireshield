package com.hireshield.entity;

public enum RiskLevel {
    LOW, CAUTION, HIGH;

    public static RiskLevel fromScore(int score) {
        if (score >= 60) {
            return HIGH;
        }
        if (score >= 30) {
            return CAUTION;
        }
        return LOW;
    }
}
