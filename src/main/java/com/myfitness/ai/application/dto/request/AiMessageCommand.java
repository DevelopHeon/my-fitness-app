package com.myfitness.ai.application.dto.request;

public record AiMessageCommand(
        String message,
        AiClientContext clientContext
) {}
