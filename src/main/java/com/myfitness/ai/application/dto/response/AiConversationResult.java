package com.myfitness.ai.application.dto.response;

import com.myfitness.ai.domain.model.AiConversation;
import java.time.Instant;

public record AiConversationResult(
        Long id,
        String title,
        Instant createdAt,
        Instant updatedAt
) {
    public static AiConversationResult from(AiConversation conversation) {
        return new AiConversationResult(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt());
    }
}
