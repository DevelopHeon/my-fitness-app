package com.myfitness.nutrition.application.usecase;

import com.myfitness.nutrition.application.usecase.response.DailyNutritionResult;
import java.time.LocalDate;
import java.util.List;

public interface NutritionInsightUseCase {
    Day getDay(Long userId, LocalDate date);

    record Day(DailyNutritionResult daily, List<String> recentFoods, List<String> frequentFoods) {}
}
