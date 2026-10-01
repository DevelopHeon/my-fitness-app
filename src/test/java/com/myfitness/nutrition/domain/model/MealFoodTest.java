package com.myfitness.nutrition.domain.model;

import static org.assertj.core.api.Assertions.*;

import com.myfitness.nutrition.domain.exception.NutritionRuleException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MealFoodTest {
    @Test
    void recordsFinalCaloriesAndOptionalMacrosWithoutCatalog() {
        Instant now = Instant.now();
        Meal meal = Meal.create(1L, LocalDate.now(), MealType.LUNCH, now);
        MealFood item = MealFood.create(meal, " 밥 ", new BigDecimal("300"), null, BigDecimal.ZERO, null, now);
        assertThat(item.getFoodName()).isEqualTo("밥");
        assertThat(item.getCalories()).isEqualByComparingTo("300");
        assertThat(item.getCarbohydrateGrams()).isNull();
        assertThat(item.getProteinGrams()).isZero();
        assertThatThrownBy(() -> item.update(meal, "밥", new BigDecimal("-1"), null, null, null, now))
                .isInstanceOf(NutritionRuleException.class);
        assertThat(item.getCalories()).isEqualByComparingTo("300");
    }
}
