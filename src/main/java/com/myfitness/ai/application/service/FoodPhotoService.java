package com.myfitness.ai.application.service;

import com.myfitness.ai.application.dto.request.FoodPhotoCommand;
import com.myfitness.ai.application.dto.response.AiFoodPhotoResult;
import com.myfitness.ai.application.exception.AiProviderUnavailableException;
import com.myfitness.ai.application.port.in.FoodPhotoUseCase;
import com.myfitness.ai.application.port.out.FoodPhotoGateway;
import com.myfitness.ai.application.port.out.FoodPhotoGateway.PhotoResponse;
import com.myfitness.ai.application.support.AiMessageResultMapper;
import com.myfitness.ai.domain.model.AiMessage;
import com.myfitness.ai.domain.model.FoodPhotoAnalysis;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FoodPhotoService implements FoodPhotoUseCase {
    private final FoodPhotoGateway gateway;
    private final FoodPhotoTransactionService transactions;
    private final AiConversationService conversations;
    private final AiMessageResultMapper messageMapper;

    public FoodPhotoService(
            FoodPhotoGateway gateway,
            FoodPhotoTransactionService transactions,
            AiConversationService conversations,
            AiMessageResultMapper messageMapper) {
        this.gateway = gateway;
        this.transactions = transactions;
        this.conversations = conversations;
        this.messageMapper = messageMapper;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AiFoodPhotoResult analyze(Long userId, Long conversationId, FoodPhotoCommand command) {
        conversations.requireOwned(userId, conversationId);
        byte[] image = gateway.prepare(command);
        FoodPhotoTransactionService.PhotoMessageWrite write = transactions.savePhotoUser(userId, conversationId);
        long started = System.nanoTime();
        PhotoResponse response = analyzeAndLogFailure(userId, conversationId, write.userMessage(), image, started);
        AiMessage assistant = transactions.savePhotoSuccess(
                userId,
                conversationId,
                write.userMessage(),
                response,
                guidance(response.analysis()),
                elapsedMillis(started));
        return AiFoodPhotoResult.from(
                write.conversation(),
                messageMapper.map(write.userMessage()),
                messageMapper.map(assistant));
    }

    private PhotoResponse analyzeAndLogFailure(
            Long userId, Long conversationId, AiMessage userMessage, byte[] image, long started) {
        try {
            return gateway.analyze(image);
        } catch (RuntimeException exception) {
            String errorCode = exception instanceof AiProviderUnavailableException failure
                    ? failure.getErrorCode() : "TRANSPORT_ERROR";
            transactions.savePhotoFailure(
                    userId, conversationId, userMessage, gateway.model(), errorCode, elapsedMillis(started));
            throw exception;
        }
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
