package com.myfitness.ai.presentation.dto.response;

import com.myfitness.ai.application.port.in.AiConversationView;
import java.time.Instant;

public record AiConversationResponse(
        Long id,
        String title,
        Instant createdAt,
        Instant updatedAt
) {
    public static AiConversationResponse from(
            AiConversationView result) {
        return new AiConversationResponse(
                result.id(),
                result.title(),
                result.createdAt(),
                result.updatedAt());
    }
}
