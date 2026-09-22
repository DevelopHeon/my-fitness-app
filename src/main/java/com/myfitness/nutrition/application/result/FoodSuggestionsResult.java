package com.myfitness.nutrition.application.result;

import com.myfitness.nutrition.domain.model.Food;
import java.util.List;

public record FoodSuggestionsResult(
        List<Food> recent,
        List<Food> frequent
) {}
