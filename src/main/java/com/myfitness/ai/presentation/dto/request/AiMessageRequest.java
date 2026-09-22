package com.myfitness.ai.presentation.dto.request;

import com.myfitness.ai.application.command.AiMessageCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiMessageRequest(
        @NotBlank
        @Size(max = 1000)
        String message,
        AiClientContextRequest clientContext
) {
    public AiMessageCommand toCommand() {
        return new AiMessageCommand(
                message,
                clientContext == null
                        ? null
                        : clientContext.toCommand());
    }
}
