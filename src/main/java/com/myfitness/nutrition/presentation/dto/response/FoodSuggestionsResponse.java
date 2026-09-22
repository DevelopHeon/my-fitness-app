package com.myfitness.nutrition.presentation.dto.response;

import com.myfitness.nutrition.application.result.FoodSuggestionsResult;
import java.util.List;

public record FoodSuggestionsResponse(
        List<FoodResponse> recent,
        List<FoodResponse> frequent
) {
    public static FoodSuggestionsResponse from(
            FoodSuggestionsResult result) {
        return new FoodSuggestionsResponse(
                result.recent().stream()
                        .map(FoodResponse::from)
                        .toList(),
                result.frequent().stream()
                        .map(FoodResponse::from)
                        .toList());
    }
}
