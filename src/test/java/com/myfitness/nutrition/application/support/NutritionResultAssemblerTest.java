package com.myfitness.nutrition.application.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.myfitness.nutrition.application.dto.response.DailyNutritionResult;
import com.myfitness.nutrition.domain.model.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class NutritionResultAssemblerTest {
    @Test
    void combinesKnownAndUnknownNutrientsIndependently() {
        Instant now = Instant.now();
        LocalDate date = LocalDate.now();
        Meal breakfast = Meal.create(1L, date, MealType.BREAKFAST, now);
        Meal dinner = Meal.create(1L, date, MealType.DINNER, now);
        MealFood first = MealFood.create(breakfast, "밥", new BigDecimal("300"), null,
                new BigDecimal("20"), BigDecimal.ZERO, now);
        MealFood second = MealFood.create(dinner, "고기", new BigDecimal("400"), BigDecimal.ZERO,
                new BigDecimal("50"), null, now);
        NutritionGoal goal = NutritionGoal.create(1L, new BigDecimal("2000"), new BigDecimal("250"),
                new BigDecimal("150"), new BigDecimal("60"), now);
        DailyNutritionResult result = new NutritionResultAssembler().daily(date, List.of(first, second), goal);
        assertThat(result.consumed().calories()).isEqualByComparingTo("700");
        assertThat(result.consumed().proteinGrams()).isEqualByComparingTo("70");
        assertThat(result.remaining().proteinGrams()).isEqualByComparingTo("80");
        assertThat(result.consumed().carbohydrateGrams()).isNull();
        assertThat(result.remaining().fatGrams()).isNull();
        assertThat(result.meals().get(1).total().fatGrams()).isZero();
    }
}
