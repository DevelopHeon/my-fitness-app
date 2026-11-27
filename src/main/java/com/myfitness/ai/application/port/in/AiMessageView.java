package com.myfitness.ai.application.port.in;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;

public record AiMessageView(
        Long id,
        String role,
        String queryType,
        String content,
        Instant createdAt,
        PhotoView foodPhotoResult
) {

    public record PhotoView(String status, List<PhotoItem> items) {}

    public record PhotoItem(String foodName, String servingDescription, BigDecimal caloriesPerServing) {}
}
