package com.myfitness.ai.presentation.dto.response;

import com.myfitness.ai.application.dto.response.AiConversationResult;
import java.time.Instant;

public record AiConversationResponse(
        Long id,
        String title,
        Instant createdAt,
        Instant updatedAt
) {
    public static AiConversationResponse from(
            AiConversationResult result) {
        return new AiConversationResponse(
                result.id(),
                result.title(),
                result.createdAt(),
                result.updatedAt());
    }
}
