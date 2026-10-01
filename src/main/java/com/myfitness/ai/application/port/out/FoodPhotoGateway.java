package com.myfitness.ai.application.port.out;

import com.myfitness.ai.application.dto.request.FoodPhotoCommand;
import com.myfitness.ai.domain.model.FoodPhotoAnalysis;

public interface FoodPhotoGateway {
    byte[] prepare(FoodPhotoCommand command);

    PhotoResponse analyze(byte[] normalizedImage);

    String model();

    record PhotoResponse(FoodPhotoAnalysis analysis, String model,
            Integer inputTokens, Integer outputTokens, Integer totalTokens) {}
}
