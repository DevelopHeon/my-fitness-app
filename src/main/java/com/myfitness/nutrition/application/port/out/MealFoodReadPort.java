package com.myfitness.nutrition.application.port.out;

import com.myfitness.nutrition.application.usecase.response.MealFoodResult;
import java.time.LocalDate;
import java.util.List;

public interface MealFoodReadPort {
    List<MealFoodResult> listDailyItems(Long userId, LocalDate date);

    List<MealFoodResult> listItemsBetween(Long userId, LocalDate from, LocalDate to);

    List<String> listRecentUsageHistory(Long userId, int limit);
}
