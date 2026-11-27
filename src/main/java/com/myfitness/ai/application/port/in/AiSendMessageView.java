package com.myfitness.ai.application.port.in;


public record AiSendMessageView(
        AiConversationView conversation,
        AiMessageView userMessage,
        AiMessageView assistantMessage,
        boolean providerCalled,
        String policyDecision
) {}
