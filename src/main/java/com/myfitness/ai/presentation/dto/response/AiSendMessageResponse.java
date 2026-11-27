package com.myfitness.ai.presentation.dto.response;

import com.myfitness.ai.application.port.in.AiSendMessageView;

public record AiSendMessageResponse(
        AiConversationResponse conversation,
        AiMessageResponse userMessage,
        AiMessageResponse assistantMessage,
        boolean providerCalled,
        String policyDecision
) {
    public static AiSendMessageResponse from(
            AiSendMessageView result) {
        return new AiSendMessageResponse(
                AiConversationResponse.from(result.conversation()),
                AiMessageResponse.from(result.userMessage()),
                AiMessageResponse.from(result.assistantMessage()),
                result.providerCalled(), result.policyDecision());
    }
}
