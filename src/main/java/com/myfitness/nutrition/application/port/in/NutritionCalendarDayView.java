package com.myfitness.nutrition.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record NutritionCalendarDayView(
        LocalDate date,
        BigDecimal calories,
        List<String> mealTypes
) {}
