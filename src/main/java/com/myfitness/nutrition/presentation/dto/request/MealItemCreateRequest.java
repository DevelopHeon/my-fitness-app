package com.myfitness.nutrition.presentation.dto.request;

import com.myfitness.nutrition.domain.model.MealType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MealItemCreateRequest(
        @NotNull LocalDate mealDate,
        @NotNull MealType mealType,
        @NotNull @Positive Long foodId,
        @NotNull @DecimalMin("0.01") @DecimalMax("100.00")
        @Digits(integer = 3, fraction = 2)
        BigDecimal servings
) {}
