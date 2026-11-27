package com.myfitness.ai.presentation.dto.request;

import com.myfitness.ai.application.port.in.AiCoachPort.MessageInput;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiMessageRequest(
        @NotBlank
        @Size(max = 1000)
        String message,
        AiClientContextRequest clientContext
) {
    public MessageInput toCommand() {
        return new MessageInput(
                message,
                clientContext == null
                        ? null
                        : clientContext.toCommand());
    }
}
