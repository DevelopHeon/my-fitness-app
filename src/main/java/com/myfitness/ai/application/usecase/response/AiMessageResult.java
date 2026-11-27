package com.myfitness.ai.application.usecase.response;

import com.myfitness.ai.domain.model.AiMessage;
import com.myfitness.ai.domain.model.FoodPhotoAnalysis;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;

public record AiMessageResult(
        Long id,
        String role,
        String queryType,
        String content,
        Instant createdAt,
        PhotoResult foodPhotoResult
) {
    public static AiMessageResult from(AiMessage message, FoodPhotoAnalysis analysis) {
        return new AiMessageResult(
                message.getId(),
                message.getRole().name(),
                message.getQueryType().name(),
                message.getContent(),
                message.getCreatedAt(),
                analysis == null ? null : new PhotoResult(
                        analysis.status().name(), analysis.items().stream().map(item -> new PhotoItem(
                                item.foodName(), item.servingDescription(), item.caloriesPerServing())).toList()));
    }

    public record PhotoResult(String status, List<PhotoItem> items) {}

    public record PhotoItem(String foodName, String servingDescription, BigDecimal caloriesPerServing) {}
}
