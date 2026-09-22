package com.myfitness.nutrition.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myfitness.nutrition.application.result.DailyNutritionResult;
import com.myfitness.nutrition.application.result.FoodResult;
import com.myfitness.nutrition.application.result.FoodSuggestionsResult;
import com.myfitness.nutrition.application.result.NutritionTotals;
import com.myfitness.nutrition.domain.model.ServingUnit;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NutritionInsightServiceTest {

    @Test
    @DisplayName("AI Nutrition Insight는 음식 추천 계산에 최근 50개 사용 이력만 요청한다")
    void limitsUsageHistoryForAiNutritionInsight() {
        NutritionApplicationService applicationService =
                mock(NutritionApplicationService.class);
        NutritionInsightService insightService =
                new NutritionInsightService(applicationService);
        LocalDate date = LocalDate.of(2026, 9, 22);

        when(applicationService.daily(1L, date))
                .thenReturn(new DailyNutritionResult(
                        date,
                        null,
                        NutritionTotals.zero(),
                        null,
                        List.of()));
        when(applicationService.suggestions(1L, 50))
                .thenReturn(new FoodSuggestionsResult(
                        List.of(food(1L, "닭가슴살")),
                        List.of(food(2L, "계란"))));

        var insight = insightService.getDay(1L, date);

        verify(applicationService).suggestions(1L, 50);
        assertThat(insight.recentFoods())
                .containsExactly("닭가슴살");
        assertThat(insight.frequentFoods())
                .containsExactly("계란");
    }

    private static FoodResult food(Long id, String name) {
        return new FoodResult(
                id,
                name,
                new BigDecimal("100"),
                ServingUnit.G,
                new BigDecimal("100"),
                BigDecimal.ZERO,
                new BigDecimal("20"),
                BigDecimal.ZERO,
                Instant.parse("2026-09-22T00:00:00Z"),
                Instant.parse("2026-09-22T00:00:00Z"));
    }
}
