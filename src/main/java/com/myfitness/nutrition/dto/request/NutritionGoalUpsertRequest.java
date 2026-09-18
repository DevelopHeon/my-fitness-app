package com.myfitness.nutrition.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record NutritionGoalUpsertRequest(
        @NotNull @DecimalMin("0.01") @Digits(integer = 6, fraction = 2)
        BigDecimal calories,
        @NotNull @DecimalMin("0.00") @Digits(integer = 6, fraction = 2)
        BigDecimal carbohydrateGrams,
        @NotNull @DecimalMin("0.00") @Digits(integer = 6, fraction = 2)
        BigDecimal proteinGrams,
        @NotNull @DecimalMin("0.00") @Digits(integer = 6, fraction = 2)
        BigDecimal fatGrams
) {}
