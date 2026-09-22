package com.myfitness.ai.application.result;

public record AiSendMessageResult(
        AiConversationResult conversation,
        AiMessageResult userMessage,
        AiMessageResult assistantMessage,
        boolean providerCalled
) {}
