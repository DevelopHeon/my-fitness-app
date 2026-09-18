package com.myfitness.nutrition.dto.response;

import java.util.List;

public record FoodSuggestionsResponse(
        List<FoodResponse> recent,
        List<FoodResponse> frequent
) {}
