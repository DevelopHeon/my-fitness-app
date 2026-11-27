package com.myfitness.nutrition.application.service;

import com.myfitness.nutrition.application.usecase.response.DailyNutritionResult;
import com.myfitness.nutrition.application.port.out.MealFoodReadPort;
import com.myfitness.nutrition.application.usecase.NutritionInsightUseCase;
import com.myfitness.nutrition.application.usecase.NutritionUseCase;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NutritionInsightService implements NutritionInsightUseCase {
    private static final int AI_USAGE_HISTORY_LIMIT = 50;
    private final NutritionUseCase nutrition;
    private final MealFoodReadPort mealFoodRead;

    public NutritionInsightService(
            NutritionUseCase nutrition, MealFoodReadPort mealFoodRead) {
        this.nutrition = nutrition;
        this.mealFoodRead = mealFoodRead;
    }

    @Override
    public Day getDay(Long userId, LocalDate date) {
        DailyNutritionResult daily =
                nutrition.daily(userId, date);
        List<String> history = mealFoodRead.listRecentUsageHistory(userId, AI_USAGE_HISTORY_LIMIT);
        Map<String, Long> counts = history.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        List<String> recent = history.stream().distinct().limit(5).toList();
        List<String> frequent = history.stream().distinct()
                .sorted(Comparator.<String>comparingLong(counts::get).reversed()).limit(5).toList();

        return new Day(daily, recent, frequent);
    }
}
