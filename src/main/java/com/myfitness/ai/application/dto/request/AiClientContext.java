package com.myfitness.ai.application.dto.request;

import java.time.LocalDate;

public record AiClientContext(
        String screen,
        LocalDate selectedDate,
        Long resourceId
) {
    public String normalizedScreen() {
        return screen == null ? null : screen.trim().toUpperCase();
    }
}
