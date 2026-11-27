package com.myfitness.ai.application.service;

import com.myfitness.ai.application.usecase.response.AiUserMessageWriteResult;

import com.myfitness.ai.application.usecase.FoodPhotoTransactionUseCase;
import com.myfitness.ai.application.usecase.AiConversationUseCase;

import com.myfitness.ai.application.port.out.FoodPhotoGateway.PhotoUpload;
import com.myfitness.ai.application.usecase.response.AiFoodPhotoResult;
import com.myfitness.ai.application.exception.AiProviderUnavailableException;
import com.myfitness.ai.application.exception.AiProviderUnavailableException.Code;
import com.myfitness.ai.application.usecase.FoodPhotoUseCase;
import com.myfitness.ai.application.port.out.FoodPhotoGateway;
import com.myfitness.ai.application.port.out.FoodPhotoGateway.PhotoResponse;
import com.myfitness.ai.application.support.AiMessageResultMapper;
import com.myfitness.ai.application.support.AiMetrics;
import com.myfitness.ai.domain.model.AiMessage;
import com.myfitness.ai.domain.model.FoodPhotoAnalysis;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FoodPhotoService implements FoodPhotoUseCase {
    private final FoodPhotoGateway gateway;
    private final FoodPhotoTransactionUseCase transactions;
    private final AiConversationUseCase conversations;
    private final AiMessageResultMapper messageMapper;
    private final AiMetrics metrics;

    public FoodPhotoService(
            FoodPhotoGateway gateway,
            FoodPhotoTransactionUseCase transactions,
            AiConversationUseCase conversations,
            AiMessageResultMapper messageMapper,
            AiMetrics metrics) {
        this.gateway = gateway;
        this.transactions = transactions;
        this.conversations = conversations;
        this.messageMapper = messageMapper;
        this.metrics = metrics;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AiFoodPhotoResult analyze(Long userId, Long conversationId, byte[] bytes, String contentType) {
        conversations.requireOwned(userId, conversationId);
        byte[] image = gateway.prepare(new PhotoUpload(bytes, contentType));
        AiUserMessageWriteResult write = transactions.savePhotoUser(userId, conversationId);
        long started = System.nanoTime();
        PhotoResponse response = analyzeAndLogFailure(userId, conversationId, write.userMessage().id(), image, started);
        AiMessage assistant = transactions.savePhotoSuccess(
                userId,
                conversationId,
                write.userMessage().id(),
                response,
                guidance(response.analysis()),
                elapsedMillis(started));
        return new AiFoodPhotoResult(
                write.conversation(),
                write.userMessage(),
                messageMapper.map(assistant));
    }

    private PhotoResponse analyzeAndLogFailure(
            Long userId, Long conversationId, Long userMessageId, byte[] image, long started) {
        PhotoResponse response;
        try {
            response = gateway.analyze(image);
        } catch (RuntimeException exception) {
            Code errorCode = exception instanceof AiProviderUnavailableException failure
                    ? failure.getErrorCode() : Code.TRANSPORT_ERROR;
            metrics.provider("photo", "openai", "FAILURE", errorCode, elapsedMillis(started));
            transactions.savePhotoFailure(
                    userId, conversationId, userMessageId, gateway.model(), errorCode.name(), elapsedMillis(started));
            throw exception;
        }
        metrics.provider("photo", "openai", response.analysis().status().name(), Code.NONE, elapsedMillis(started));
        metrics.tokens("photo", "openai", response.inputTokens(), response.outputTokens());
        return response;
    }

    private static long elapsedMillis(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }

    private String guidance(FoodPhotoAnalysis analysis) {
        return switch (analysis.status()) {
            case FOOD -> "일반적인 1인분 기준으로 추정했어요. 사진 속 실제 양을 측정한 값은 아니에요. 기록 화면에서 수정한 뒤 저장해주세요.";
            case NOT_FOOD -> "음식 사진으로 확인되지 않았어요. 음식이 보이는 사진을 선택해주세요.";
            case UNCERTAIN -> "음식을 식별하기 어려워요. 밝고 선명하게 다시 찍거나 식단 화면에서 직접 입력해주세요.";
        };
    }
}
