package com.myfitness.nutrition.dto.request;

import com.myfitness.nutrition.domain.ServingUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record FoodUpsertRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @DecimalMin("0.01") @Digits(integer = 6, fraction = 2)
        BigDecimal servingAmount,
        @NotNull ServingUnit servingUnit,
        @NotNull @DecimalMin("0.00") @Digits(integer = 6, fraction = 2)
        BigDecimal calories,
        @NotNull @DecimalMin("0.00") @Digits(integer = 6, fraction = 2)
        BigDecimal carbohydrateGrams,
        @NotNull @DecimalMin("0.00") @Digits(integer = 6, fraction = 2)
        BigDecimal proteinGrams,
        @NotNull @DecimalMin("0.00") @Digits(integer = 6, fraction = 2)
        BigDecimal fatGrams
) {}
