package com.myfitness.ai.application.result;

import com.myfitness.ai.domain.model.AiMessage;
import java.time.Instant;

public record AiMessageResult(
        Long id,
        String role,
        String queryType,
        String content,
        Instant createdAt
) {
    public static AiMessageResult from(AiMessage message) {
        return new AiMessageResult(
                message.getId(),
                message.getRole().name(),
                message.getQueryType().name(),
                message.getContent(),
                message.getCreatedAt());
    }
}
