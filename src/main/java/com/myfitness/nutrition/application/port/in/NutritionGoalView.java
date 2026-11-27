package com.myfitness.nutrition.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;

public record NutritionGoalView(
        Long id,
        BigDecimal calories,
        BigDecimal carbohydrateGrams,
        BigDecimal proteinGrams,
        BigDecimal fatGrams,
        Instant updatedAt
) {}
