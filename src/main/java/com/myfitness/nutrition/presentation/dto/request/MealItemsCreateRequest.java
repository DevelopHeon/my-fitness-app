package com.myfitness.nutrition.presentation.dto.request;

import com.myfitness.nutrition.application.dto.request.MealItemCommand;
import com.myfitness.nutrition.domain.model.MealType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MealItemsCreateRequest(
        @NotNull LocalDate mealDate,
        @NotNull MealType mealType,
        @NotEmpty @Size(max = 20) List<@NotNull @Valid ItemRequest> items
) {
    public record ItemRequest(
            @NotBlank @Size(max = 100) String foodName,
            @NotNull @DecimalMin("0") @DecimalMax("999999.99") @Digits(integer = 6, fraction = 2)
            BigDecimal calories,
            @DecimalMin("0") @DecimalMax("999999.99") @Digits(integer = 6, fraction = 2)
            BigDecimal carbohydrateGrams,
            @DecimalMin("0") @DecimalMax("999999.99") @Digits(integer = 6, fraction = 2)
            BigDecimal proteinGrams,
            @DecimalMin("0") @DecimalMax("999999.99") @Digits(integer = 6, fraction = 2)
            BigDecimal fatGrams
    ) {
        public MealItemCommand toCommand() {
            return new MealItemCommand(foodName, calories, carbohydrateGrams, proteinGrams, fatGrams);
        }
    }
}
