package com.myfitness.ai.application.port.in;

import java.time.Instant;

public record AiConversationView(
        Long id,
        String title,
        Instant createdAt,
        Instant updatedAt
) {}
