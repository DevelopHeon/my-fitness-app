package com.myfitness.ai.presentation.dto.response;

import com.myfitness.ai.application.port.in.AiMessageView.PhotoView;
import java.math.BigDecimal;
import java.util.List;

public record FoodPhotoResponse(String status, List<Item> items) {
    public record Item(String foodName, String servingDescription, BigDecimal caloriesPerServing) {}

    public static FoodPhotoResponse from(PhotoView analysis) {
        return analysis == null ? null : new FoodPhotoResponse(analysis.status(), analysis.items().stream()
                .map(item -> new Item(item.foodName(), item.servingDescription(), item.caloriesPerServing())).toList());
    }
}
