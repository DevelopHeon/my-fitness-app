package com.myfitness.ai.application.dto.response;

import com.myfitness.ai.domain.model.AiMessage;
import com.myfitness.ai.domain.model.FoodPhotoAnalysis;
import java.time.Instant;

public record AiMessageResult(
        Long id,
        String role,
        String queryType,
        String content,
        Instant createdAt,
        FoodPhotoAnalysis foodPhotoResult
) {
    public static AiMessageResult from(AiMessage message, FoodPhotoAnalysis analysis) {
        return new AiMessageResult(
                message.getId(),
                message.getRole().name(),
                message.getQueryType().name(),
                message.getContent(),
                message.getCreatedAt(),
                analysis);
    }
}
