package com.myfitness.ai.presentation.dto.request;

import com.myfitness.ai.application.command.AiClientContext;
import java.time.LocalDate;

public record AiClientContextRequest(
        String screen,
        LocalDate selectedDate,
        Long resourceId
) {
    public AiClientContext toCommand() {
        return new AiClientContext(
                screen,
                selectedDate,
                resourceId);
    }
}
