package com.myfitness.ai.application.command;

import java.time.LocalDate;

/**
 * 변경 전 Router의 입력을 보존하는 테스트 전용 스냅샷.
 * 운영 DTO는 application.dto.request에 있으며 이 타입은 운영 소스에 포함되지 않는다.
 */
public record AiClientContext(
        String screen,
        LocalDate selectedDate,
        Long resourceId
) {
    public String normalizedScreen() {
        return screen == null ? null : screen.trim().toUpperCase();
    }
}
