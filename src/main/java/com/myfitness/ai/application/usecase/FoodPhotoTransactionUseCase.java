package com.myfitness.ai.application.usecase;

import com.myfitness.ai.application.usecase.response.AiUserMessageWriteResult;

import com.myfitness.ai.application.port.out.FoodPhotoGateway.PhotoResponse;
import com.myfitness.ai.domain.model.AiMessage;

public interface FoodPhotoTransactionUseCase {
    AiUserMessageWriteResult savePhotoUser(Long userId, Long conversationId);

    AiMessage savePhotoSuccess(
            Long userId,
            Long conversationId,
            Long userMessageId,
            PhotoResponse response,
            String content,
            long latencyMs);

    void savePhotoFailure(
            Long userId,
            Long conversationId,
            Long userMessageId,
            String model,
            String errorCode,
            long latencyMs);
}
