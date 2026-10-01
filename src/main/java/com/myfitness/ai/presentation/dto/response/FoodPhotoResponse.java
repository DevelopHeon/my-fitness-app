package com.myfitness.ai.presentation.dto.response;

import com.myfitness.ai.domain.model.FoodPhotoAnalysis;
import java.math.BigDecimal;
import java.util.List;

public record FoodPhotoResponse(String status, List<Item> items) {
    public record Item(String foodName, String servingDescription, BigDecimal caloriesPerServing) {}

    public static FoodPhotoResponse from(FoodPhotoAnalysis analysis) {
        return analysis == null ? null : new FoodPhotoResponse(analysis.status().name(), analysis.items().stream()
                .map(item -> new Item(item.foodName(), item.servingDescription(), item.caloriesPerServing())).toList());
    }
}
