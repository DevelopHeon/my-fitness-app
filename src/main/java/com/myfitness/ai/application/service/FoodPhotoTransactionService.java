package com.myfitness.ai.application.service;

import com.myfitness.ai.application.usecase.response.AiUserMessageWriteResult;

import com.myfitness.ai.application.usecase.FoodPhotoTransactionUseCase;
import com.myfitness.ai.application.usecase.AiConversationUseCase;
import com.myfitness.ai.application.usecase.response.AiConversationResult;
import com.myfitness.ai.application.usecase.response.AiMessageResult;

import com.myfitness.ai.domain.repository.AiConversationRepository;
import com.myfitness.ai.domain.repository.AiMessageRepository;
import com.myfitness.ai.domain.repository.AiRequestLogRepository;
import com.myfitness.ai.application.port.out.FoodPhotoGateway.PhotoResponse;
import com.myfitness.ai.domain.model.AiConversation;
import com.myfitness.ai.domain.model.AiMessage;
import com.myfitness.ai.domain.model.AiMessageRole;
import com.myfitness.ai.domain.model.AiQueryType;
import com.myfitness.ai.domain.model.AiRequestLog;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class FoodPhotoTransactionService implements FoodPhotoTransactionUseCase {
    private static final String PROMPT_VERSION = "food-photo-v1";
    private final AiConversationUseCase conversations;
    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final AiRequestLogRepository requestLogRepository;
    private final ObjectMapper mapper;
    private final Clock clock = Clock.systemUTC();

    public FoodPhotoTransactionService(
            AiConversationUseCase conversations,
            AiConversationRepository conversationRepository,
            AiMessageRepository messageRepository,
            AiRequestLogRepository requestLogRepository,
            ObjectMapper mapper) {
        this.conversations = conversations;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.requestLogRepository = requestLogRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public AiUserMessageWriteResult savePhotoUser(Long userId, Long conversationId) {
        AiConversation conversation = conversations.requireOwned(userId, conversationId);
        Instant now = clock.instant();
        AiMessage message = messageRepository.save(AiMessage.photo(
                conversationId, AiMessageRole.USER, "음식 사진의 1인분 칼로리 분석", null, now));
        if (conversation.hasDefaultTitle()) {
            conversation.rename("음식 사진 분석", now);
        } else {
            conversation.touch(now);
        }
        conversationRepository.save(conversation);
        return new AiUserMessageWriteResult(AiConversationResult.from(conversation), AiMessageResult.from(message, null));
    }

    @Override
    @Transactional
    public AiMessage savePhotoSuccess(
            Long userId,
            Long conversationId,
            Long userMessageId,
            PhotoResponse response,
            String content,
            long latencyMs) {
        conversations.requireOwned(userId, conversationId);
        AiMessage assistant = messageRepository.save(AiMessage.photo(
                conversationId,
                AiMessageRole.ASSISTANT,
                content,
                mapper.writeValueAsString(response.analysis()),
                clock.instant()));
        AiRequestLog log = AiRequestLog.success(
                userId,
                conversationId,
                userMessageId,
                assistant.getId(),
                AiQueryType.NUTRITION,
                "openai",
                response.model(),
                PROMPT_VERSION,
                response.inputTokens(),
                response.outputTokens(),
                response.totalTokens(),
                latencyMs,
                response.analysis().status().name(),
                clock.instant());
        requestLogRepository.save(log.foodPhoto());
        return assistant;
    }

    @Override
    @Transactional
    public void savePhotoFailure(
            Long userId,
            Long conversationId,
            Long userMessageId,
            String model,
            String errorCode,
            long latencyMs) {
        conversations.requireOwned(userId, conversationId);
        AiRequestLog log = AiRequestLog.failed(
                userId,
                conversationId,
                userMessageId,
                AiQueryType.NUTRITION,
                "openai",
                model,
                PROMPT_VERSION,
                latencyMs,
                errorCode,
                null,
                clock.instant());
        requestLogRepository.save(log.foodPhoto());
    }
}
