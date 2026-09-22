package com.myfitness.ai.presentation.dto.response;

import com.myfitness.ai.application.result.AiSendMessageResult;

public record AiSendMessageResponse(
        AiConversationResponse conversation,
        AiMessageResponse userMessage,
        AiMessageResponse assistantMessage,
        boolean providerCalled
) {
    public static AiSendMessageResponse from(
            AiSendMessageResult result) {
        return new AiSendMessageResponse(
                AiConversationResponse.from(result.conversation()),
                AiMessageResponse.from(result.userMessage()),
                AiMessageResponse.from(result.assistantMessage()),
                result.providerCalled());
    }
}
