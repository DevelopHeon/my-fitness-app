package com.myfitness.ai.application.usecase;

import com.myfitness.ai.application.usecase.response.AiFoodPhotoResult;

public interface FoodPhotoUseCase {
    AiFoodPhotoResult analyze(Long userId, Long conversationId, byte[] bytes, String contentType);
}
