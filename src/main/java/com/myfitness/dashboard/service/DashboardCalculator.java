package com.myfitness.dashboard.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class DashboardCalculator {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal THIRTY = new BigDecimal("30");

    private DashboardCalculator() {}

    static BigDecimal volume(BigDecimal weightKg, int reps) {
        BigDecimal weight = weightKg == null ? BigDecimal.ZERO : weightKg;
        return weight.multiply(BigDecimal.valueOf(reps))
                .setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal estimatedOneRepMax(BigDecimal weightKg, int reps) {
        if (weightKg == null || weightKg.signum() <= 0 || reps <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal multiplier = BigDecimal.ONE.add(
                BigDecimal.valueOf(reps)
                        .divide(THIRTY, 8, RoundingMode.HALF_UP));
        return weightKg.multiply(multiplier)
                .setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal changePercentage(
            BigDecimal current,
            BigDecimal previous) {
        if (previous == null || previous.signum() == 0) {
            return null;
        }
        BigDecimal currentValue =
                current == null ? BigDecimal.ZERO : current;
        return currentValue.subtract(previous)
                .divide(previous, 8, RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
