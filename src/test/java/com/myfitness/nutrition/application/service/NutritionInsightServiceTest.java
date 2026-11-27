package com.myfitness.nutrition.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.myfitness.nutrition.application.usecase.response.DailyNutritionResult;
import com.myfitness.nutrition.application.usecase.response.NutritionTotals;
import com.myfitness.nutrition.application.port.in.insight.NutritionInsightQuery.NutritionDayInsight;
import com.myfitness.nutrition.application.port.out.MealFoodReadPort;
import com.myfitness.nutrition.application.usecase.NutritionUseCase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class NutritionInsightServiceTest {
    @Test
    void exposesRecordedNamesWithoutCatalogAndPreservesUnknownMacros() {
        NutritionUseCase applicationService = mock(NutritionUseCase.class);
        LocalDate date = LocalDate.now();
        when(applicationService.daily(1L, date)).thenReturn(new DailyNutritionResult(date, null,
                new NutritionTotals(BigDecimal.ZERO, null, null, null), null, List.of()));
        MealFoodReadPort meals = mock(MealFoodReadPort.class);
        when(meals.listRecentUsageHistory(1L, 50)).thenReturn(List.of("밥", "고기", "고기"));
        NutritionDayInsight result = new NutritionInsightPortService(
                new NutritionInsightService(applicationService, meals)).getDay(1L, date);
        verify(meals).listRecentUsageHistory(1L, 50);
        assertThat(result.recentFoods()).containsExactly("밥", "고기");
        assertThat(result.frequentFoods()).containsExactly("고기", "밥");
        assertThat(result.consumed().proteinGrams()).isNull();
    }
}
