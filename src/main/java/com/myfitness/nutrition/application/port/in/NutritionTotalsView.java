package com.myfitness.nutrition.application.port.in;

import java.math.BigDecimal;

public record NutritionTotalsView(
        BigDecimal calories,
        BigDecimal carbohydrateGrams,
        BigDecimal proteinGrams,
        BigDecimal fatGrams
) {}
