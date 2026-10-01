package com.myfitness.nutrition.presentation.dto.request;

import com.myfitness.nutrition.domain.model.MealType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MealItemUpdateRequest(
        @NotNull LocalDate mealDate,
        @NotNull MealType mealType,
        @NotBlank @Size(max = 100) String foodName,
        @NotNull @DecimalMin("0") @DecimalMax("999999.99") @Digits(integer = 6, fraction = 2)
        BigDecimal calories,
        @DecimalMin("0") @DecimalMax("999999.99") @Digits(integer = 6, fraction = 2)
        BigDecimal carbohydrateGrams,
        @DecimalMin("0") @DecimalMax("999999.99") @Digits(integer = 6, fraction = 2)
        BigDecimal proteinGrams,
        @DecimalMin("0") @DecimalMax("999999.99") @Digits(integer = 6, fraction = 2)
        BigDecimal fatGrams
) {}
