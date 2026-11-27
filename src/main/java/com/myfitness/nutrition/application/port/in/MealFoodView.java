package com.myfitness.nutrition.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record MealFoodView(
        Long id,
        LocalDate mealDate,
        String mealType,
        String foodName,
        BigDecimal calories,
        BigDecimal carbohydrateGrams,
        BigDecimal proteinGrams,
        BigDecimal fatGrams,
        Instant createdAt,
        Instant updatedAt
) {}
