package com.myfitness.ai.application.dto.response;

public record AiSendMessageResult(
        AiConversationResult conversation,
        AiMessageResult userMessage,
        AiMessageResult assistantMessage,
        boolean providerCalled,
        String policyDecision
) {}
