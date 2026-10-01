package com.myfitness.nutrition.application.dto.request;

import java.math.BigDecimal;

public record MealItemCommand(
        String foodName,
        BigDecimal calories,
        BigDecimal carbohydrateGrams,
        BigDecimal proteinGrams,
        BigDecimal fatGrams
) {}
