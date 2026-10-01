package com.myfitness.ai.application.service;

import com.myfitness.ai.application.port.out.AiConversationRepositoryPort;
import com.myfitness.ai.application.port.out.AiMessageRepositoryPort;
import com.myfitness.ai.application.port.out.AiRequestLogRepositoryPort;
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
public class FoodPhotoTransactionService {
    private static final String PROMPT_VERSION = "food-photo-v1";
    private final AiConversationService conversations;
    private final AiConversationRepositoryPort conversationRepository;
    private final AiMessageRepositoryPort messageRepository;
    private final AiRequestLogRepositoryPort requestLogRepository;
    private final ObjectMapper mapper;
    private final Clock clock = Clock.systemUTC();

    public FoodPhotoTransactionService(
            AiConversationService conversations,
            AiConversationRepositoryPort conversationRepository,
            AiMessageRepositoryPort messageRepository,
            AiRequestLogRepositoryPort requestLogRepository,
            ObjectMapper mapper) {
        this.conversations = conversations;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.requestLogRepository = requestLogRepository;
        this.mapper = mapper;
    }

    @Transactional
    public PhotoMessageWrite savePhotoUser(Long userId, Long conversationId) {
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
        return new PhotoMessageWrite(conversation, message);
    }

    @Transactional
    public AiMessage savePhotoSuccess(
            Long userId,
            Long conversationId,
            AiMessage user,
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
                user.getId(),
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

    @Transactional
    public void savePhotoFailure(
            Long userId,
            Long conversationId,
            AiMessage user,
            String model,
            String errorCode,
            long latencyMs) {
        conversations.requireOwned(userId, conversationId);
        AiRequestLog log = AiRequestLog.failed(
                userId,
                conversationId,
                user.getId(),
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

    public record PhotoMessageWrite(AiConversation conversation, AiMessage userMessage) {}
}
