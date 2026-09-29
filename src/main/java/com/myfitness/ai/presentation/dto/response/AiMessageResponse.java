package com.myfitness.ai.presentation.dto.response;

import com.myfitness.ai.application.dto.response.AiMessageResult;
import java.time.Instant;

public record AiMessageResponse(
        Long id,
        String role,
        String queryType,
        String content,
        Instant createdAt
) {
    public static AiMessageResponse from(AiMessageResult result) {
        return new AiMessageResponse(
                result.id(),
                result.role(),
                result.queryType(),
                result.content(),
                result.createdAt());
    }
}
