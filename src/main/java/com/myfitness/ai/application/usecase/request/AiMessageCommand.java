package com.myfitness.ai.application.usecase.request;

public record AiMessageCommand(
        String message,
        AiClientContext clientContext
) {}
