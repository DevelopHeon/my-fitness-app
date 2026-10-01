package com.myfitness.ai.application.dto.response;

import com.myfitness.ai.domain.model.AiConversation;

public record AiSendMessageResult(
        AiConversationResult conversation,
        AiMessageResult userMessage,
        AiMessageResult assistantMessage,
        boolean providerCalled,
        String policyDecision
) {
    public static AiSendMessageResult from(
            AiConversation conversation,
            AiMessageResult userMessage,
            AiMessageResult assistantMessage,
            boolean providerCalled,
            String policyDecision) {
        return new AiSendMessageResult(
                AiConversationResult.from(conversation),
                userMessage,
                assistantMessage,
                providerCalled,
                policyDecision);
    }
}
