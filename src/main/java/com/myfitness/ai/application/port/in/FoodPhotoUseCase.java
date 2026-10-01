package com.myfitness.ai.application.port.in;

import com.myfitness.ai.application.dto.request.FoodPhotoCommand;
import com.myfitness.ai.application.dto.response.AiFoodPhotoResult;

public interface FoodPhotoUseCase {
    AiFoodPhotoResult analyze(Long userId, Long conversationId, FoodPhotoCommand command);
}
