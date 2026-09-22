package com.myfitness.ai.application.port.out;

import com.myfitness.ai.domain.model.AiMessageRole;
import java.util.List;

public interface AiChatGateway {
    AiModelResponse chat(AiModelRequest request);

    String provider();

    String model();

    record AiModelRequest(
            String systemPrompt,
            String context,
            List<HistoryMessage> history,
            String userMessage
    ) {}

    record HistoryMessage(
            AiMessageRole role,
            String content
    ) {}

    record AiModelResponse(
            String content,
            String provider,
            String model,
            Integer inputTokens,
            Integer outputTokens,
            Integer totalTokens
    ) {}
}
