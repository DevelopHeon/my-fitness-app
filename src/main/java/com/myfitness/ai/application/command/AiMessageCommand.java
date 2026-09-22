package com.myfitness.ai.application.command;

public record AiMessageCommand(
        String message,
        AiClientContext clientContext
) {}
