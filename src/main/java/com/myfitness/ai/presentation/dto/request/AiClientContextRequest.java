package com.myfitness.ai.presentation.dto.request;

import com.myfitness.ai.application.port.in.AiCoachPort.ClientContext;
import java.time.LocalDate;

public record AiClientContextRequest(
        String screen,
        LocalDate selectedDate,
        Long resourceId
) {
    public ClientContext toCommand() {
        return new ClientContext(
                screen,
                selectedDate,
                resourceId);
    }
}
