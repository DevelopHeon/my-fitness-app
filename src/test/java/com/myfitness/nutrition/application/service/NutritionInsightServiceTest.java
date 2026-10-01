package com.myfitness.nutrition.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.myfitness.nutrition.application.dto.response.DailyNutritionResult;
import com.myfitness.nutrition.application.dto.response.NutritionTotals;
import com.myfitness.nutrition.application.port.in.insight.NutritionInsightQuery.NutritionDayInsight;
import com.myfitness.nutrition.domain.model.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class NutritionInsightServiceTest {
    @Test
    void exposesRecordedNamesWithoutCatalogAndPreservesUnknownMacros() {
        NutritionApplicationService applicationService = mock(NutritionApplicationService.class);
        LocalDate date = LocalDate.now();
        when(applicationService.daily(1L, date)).thenReturn(new DailyNutritionResult(date, null,
                new NutritionTotals(BigDecimal.ZERO, null, null, null), null, List.of()));
        MealService meals = mock(MealService.class);
        Meal meal = Meal.create(1L, date, MealType.LUNCH, Instant.now());
        MealFood rice = MealFood.create(meal, "밥", BigDecimal.ZERO, null, null, null, Instant.now());
        MealFood meat = MealFood.create(meal, "고기", BigDecimal.ZERO, null, null, null, Instant.now());
        when(meals.listRecentUsageHistory(1L, 50)).thenReturn(List.of(rice, meat, meat));
        NutritionDayInsight result = new NutritionInsightService(applicationService, meals).getDay(1L, date);
        verify(meals).listRecentUsageHistory(1L, 50);
        assertThat(result.recentFoods()).containsExactly("밥", "고기");
        assertThat(result.frequentFoods()).containsExactly("고기", "밥");
        assertThat(result.consumed().proteinGrams()).isNull();
    }
}
