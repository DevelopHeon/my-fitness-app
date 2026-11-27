package com.myfitness.ai.application.usecase.response;

public record AiSendMessageResult(
        AiConversationResult conversation,
        AiMessageResult userMessage,
        AiMessageResult assistantMessage,
        boolean providerCalled,
        String policyDecision
) {}
