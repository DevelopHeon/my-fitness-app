package com.myfitness.nutrition.application.dto.response;

import java.util.List;

public record FoodSuggestionsResult(
        List<FoodResult> recent,
        List<FoodResult> frequent
) {}
