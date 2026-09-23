package com.myfitness.ai.application.service;

import com.myfitness.ai.application.config.AiCoachProperties;
import com.myfitness.ai.application.context.AiContextBundle;
import com.myfitness.ai.application.exception.AiConversationAccessException;
import com.myfitness.ai.application.exception.AiConversationNotFoundException;
import com.myfitness.ai.application.port.out.AiChatGateway.AiModelResponse;
import com.myfitness.ai.application.port.out.AiConversationRepositoryPort;
import com.myfitness.ai.application.port.out.AiMessageRepositoryPort;
import com.myfitness.ai.application.port.out.AiRequestLogRepositoryPort;
import com.myfitness.ai.domain.model.AiConversation;
import com.myfitness.ai.domain.model.AiMessage;
import com.myfitness.ai.domain.model.AiQueryType;
import com.myfitness.ai.domain.model.AiRequestLog;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AiMessageTransactionService {
    private final AiConversationRepositoryPort conversationRepository;
    private final AiMessageRepositoryPort messageRepository;
    private final AiRequestLogRepositoryPort requestLogRepository;
    private final AiCoachProperties properties;
    private final Clock clock;

    @Autowired
    public AiMessageTransactionService(
            AiConversationRepositoryPort conversationRepository,
            AiMessageRepositoryPort messageRepository,
            AiRequestLogRepositoryPort requestLogRepository,
            AiCoachProperties properties) {
        this(
                conversationRepository,
                messageRepository,
                requestLogRepository,
                properties,
                Clock.systemUTC());
    }

    AiMessageTransactionService(
            AiConversationRepositoryPort conversationRepository,
            AiMessageRepositoryPort messageRepository,
            AiRequestLogRepositoryPort requestLogRepository,
            AiCoachProperties properties,
            Clock clock) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.requestLogRepository = requestLogRepository;
        this.properties = properties;
        this.clock = clock;
    }

    public List<AiMessage> loadMessagesForOwnedConversation(
            Long userId,
            Long conversationId) {
        requireOwned(userId, conversationId);
        return messageRepository.findAllByConversationId(conversationId);
    }

    @Transactional
    public UserMessageWrite saveUserMessage(
            Long userId,
            Long conversationId,
            AiQueryType queryType,
            String message) {
        AiConversation conversation = requireOwned(userId, conversationId);
        Instant now = clock.instant();

        AiMessage userMessage = messageRepository.save(
                AiMessage.user(
                        conversationId,
                        queryType,
                        message,
                        now));

        if (conversation.hasDefaultTitle()) {
            conversation.rename(titleFrom(message), now);
        } else {
            conversation.touch(now);
        }
        conversationRepository.save(conversation);

        return new UserMessageWrite(conversation, userMessage);
    }

    @Transactional
    public AiMessage saveRejectedResponse(
            Long userId,
            Long conversationId,
            AiMessage userMessage,
            AiQueryType queryType,
            String content) {
        AiMessage assistantMessage = messageRepository.save(
                AiMessage.assistant(
                        conversationId,
                        queryType,
                        content,
                        clock.instant()));

        requestLogRepository.save(AiRequestLog.rejected(
                userId,
                conversationId,
                userMessage.getId(),
                queryType,
                properties.getPromptVersion(),
                clock.instant()));

        return assistantMessage;
    }

    @Transactional
    public AiMessage saveProviderSuccess(
            Long userId,
            Long conversationId,
            AiMessage userMessage,
            AiQueryType queryType,
            AiContextBundle context,
            AiModelResponse response,
            long latencyMs) {
        AiMessage assistantMessage = messageRepository.save(
                AiMessage.assistant(
                        conversationId,
                        queryType,
                        response.content(),
                        clock.instant()));

        requestLogRepository.save(AiRequestLog.success(
                userId,
                conversationId,
                userMessage.getId(),
                assistantMessage.getId(),
                queryType,
                response.provider(),
                response.model(),
                properties.getPromptVersion(),
                response.inputTokens(),
                response.outputTokens(),
                response.totalTokens(),
                latencyMs,
                context.typeNames(),
                clock.instant()));

        return assistantMessage;
    }

    @Transactional
    public void saveProviderFailure(
            Long userId,
            Long conversationId,
            AiMessage userMessage,
            AiQueryType queryType,
            AiContextBundle context,
            String provider,
            String model,
            RuntimeException exception,
            long latencyMs) {
        requestLogRepository.save(AiRequestLog.failed(
                userId,
                conversationId,
                userMessage.getId(),
                queryType,
                provider,
                model,
                properties.getPromptVersion(),
                latencyMs,
                exception.getClass().getSimpleName(),
                context.typeNames(),
                clock.instant()));
    }

    private AiConversation requireOwned(Long userId, Long conversationId) {
        AiConversation conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(AiConversationNotFoundException::new);
        if (!conversation.belongsTo(userId)) {
            throw new AiConversationAccessException();
        }
        return conversation;
    }

    private String titleFrom(String message) {
        String normalized = message
                .replaceAll("\\s+", " ")
                .trim();
        int limit = Math.min(
                properties.getTitleMaxLength(),
                100);
        if (normalized.length() <= limit) {
            return normalized;
        }
        return normalized.substring(0, limit);
    }

    public record UserMessageWrite(
            AiConversation conversation,
            AiMessage userMessage
    ) {
    }
}
