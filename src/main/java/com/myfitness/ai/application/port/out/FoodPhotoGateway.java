package com.myfitness.ai.application.port.out;

import com.myfitness.ai.domain.model.FoodPhotoAnalysis;

public interface FoodPhotoGateway {
    byte[] prepare(PhotoUpload upload);

    PhotoResponse analyze(byte[] normalizedImage);

    String model();

    record PhotoUpload(byte[] bytes, String contentType) {}

    record PhotoResponse(FoodPhotoAnalysis analysis, String model,
            Integer inputTokens, Integer outputTokens, Integer totalTokens) {}
}
