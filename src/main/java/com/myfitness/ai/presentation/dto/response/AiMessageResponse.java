package com.myfitness.ai.presentation.dto.response;

import com.myfitness.ai.application.port.in.AiMessageView;
import java.time.Instant;

public record AiMessageResponse(
        Long id,
        String role,
        String queryType,
        String content,
        Instant createdAt,
        FoodPhotoResponse foodPhotoResult
) {
    public static AiMessageResponse from(AiMessageView result) {
        return new AiMessageResponse(
                result.id(),
                result.role(),
                result.queryType(),
                result.content(),
                result.createdAt(),
                FoodPhotoResponse.from(result.foodPhotoResult()));
    }
}
