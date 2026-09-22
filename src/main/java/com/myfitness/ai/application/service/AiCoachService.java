package com.myfitness.ai.application.service;

import com.myfitness.ai.application.command.AiClientContext;
import com.myfitness.ai.application.command.AiMessageCommand;
import com.myfitness.ai.application.config.AiCoachProperties;
import com.myfitness.ai.application.context.AiContextBuilder;
import com.myfitness.ai.application.context.AiContextBundle;
import com.myfitness.ai.application.exception.AiConversationAccessException;
import com.myfitness.ai.application.exception.AiConversationNotFoundException;
import com.myfitness.ai.application.port.in.AiCoachUseCase;
import com.myfitness.ai.application.port.out.AiConversationRepositoryPort;
import com.myfitness.ai.application.port.out.AiMessageRepositoryPort;
import com.myfitness.ai.application.port.out.AiRequestLogRepositoryPort;
import com.myfitness.ai.application.result.AiConversationResult;
import com.myfitness.ai.application.result.AiMessageResult;
import com.myfitness.ai.application.result.AiSendMessageResult;
import com.myfitness.ai.application.router.AiQueryRouter;
import com.myfitness.ai.domain.exception.AiRuleException;
import com.myfitness.ai.domain.model.AiConversation;
import com.myfitness.ai.domain.model.AiMessage;
import com.myfitness.ai.domain.model.AiQueryType;
import com.myfitness.ai.domain.model.AiRequestLog;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AiCoachService implements AiCoachUseCase {
    private static final String OUT_OF_SCOPE_MESSAGE =
            "My Fitness AI Coach에서는 운동, 신체 기록, 식단 및 영양과 관련된 질문을 도와드릴 수 있습니다.";

    private final AiConversationRepositoryPort conversationRepository;
    private final AiMessageRepositoryPort messageRepository;
    private final AiRequestLogRepositoryPort requestLogRepository;
    private final AiQueryRouter queryRouter;
    private final AiContextBuilder contextBuilder;
    private final AiHistorySelector historySelector;
    private final AiProviderExecutor providerExecutor;
    private final AiCoachProperties properties;
    private final Clock clock;

    @Autowired
    public AiCoachService(
            AiConversationRepositoryPort conversationRepository,
            AiMessageRepositoryPort messageRepository,
            AiRequestLogRepositoryPort requestLogRepository,
            AiQueryRouter queryRouter,
            AiContextBuilder contextBuilder,
            AiHistorySelector historySelector,
            AiProviderExecutor providerExecutor,
            AiCoachProperties properties) {
        this(
                conversationRepository,
                messageRepository,
                requestLogRepository,
                queryRouter,
                contextBuilder,
                historySelector,
                providerExecutor,
                properties,
                Clock.systemUTC());
    }

    AiCoachService(
            AiConversationRepositoryPort conversationRepository,
            AiMessageRepositoryPort messageRepository,
            AiRequestLogRepositoryPort requestLogRepository,
            AiQueryRouter queryRouter,
            AiContextBuilder contextBuilder,
            AiHistorySelector historySelector,
            AiProviderExecutor providerExecutor,
            AiCoachProperties properties,
            Clock clock) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.requestLogRepository = requestLogRepository;
        this.queryRouter = queryRouter;
        this.contextBuilder = contextBuilder;
        this.historySelector = historySelector;
        this.providerExecutor = providerExecutor;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public List<AiConversationResult> listConversations(Long userId) {
        return conversationRepository.findAllByUserId(userId).stream()
                .map(AiConversationResult::from)
                .toList();
    }

    @Override
    public AiConversationResult createConversation(Long userId) {
        AiConversation conversation = conversationRepository.save(
                AiConversation.create(userId, clock.instant()));
        return AiConversationResult.from(conversation);
    }

    @Override
    public AiConversationResult renameConversation(
            Long userId,
            Long conversationId,
            String title) {
        AiConversation conversation = getOwned(userId, conversationId);
        conversation.rename(title, clock.instant());
        return AiConversationResult.from(
                conversationRepository.save(conversation));
    }

    @Override
    public void deleteConversation(Long userId, Long conversationId) {
        AiConversation conversation = getOwned(userId, conversationId);
        requestLogRepository.deleteAllByConversationId(conversationId);
        messageRepository.deleteAllByConversationId(conversationId);
        conversationRepository.delete(conversation);
    }

    @Override
    public List<AiMessageResult> listMessages(
            Long userId,
            Long conversationId) {
        getOwned(userId, conversationId);
        return messageRepository.findAllByConversationId(conversationId)
                .stream()
                .map(AiMessageResult::from)
                .toList();
    }

    @Override
    public AiSendMessageResult sendMessage(
            Long userId,
            Long conversationId,
            AiMessageCommand command) {
        AiConversation conversation = getOwned(userId, conversationId);
        String message = validateMessage(
                command == null ? null : command.message());
        AiClientContext clientContext =
                command == null ? null : command.clientContext();

        List<AiMessage> previousMessages =
                messageRepository.findAllByConversationId(conversationId);
        AiQueryType queryType = queryRouter.route(
                message,
                clientContext,
                historySelector.latestUserQueryType(previousMessages));

        AiMessage userMessage = saveUserMessage(
                conversation,
                queryType,
                message);

        if (queryType == AiQueryType.OUT_OF_SCOPE) {
            return respondOutOfScope(
                    userId,
                    conversation,
                    userMessage,
                    queryType);
        }

        AiContextBundle context = contextBuilder.build(
                userId,
                queryType,
                clientContext,
                message);
        AiMessage assistantMessage = providerExecutor.generate(
                userId,
                conversationId,
                userMessage,
                queryType,
                context,
                historySelector.select(previousMessages, queryType),
                message);

        return result(
                conversation,
                userMessage,
                assistantMessage,
                true);
    }

    private AiMessage saveUserMessage(
            AiConversation conversation,
            AiQueryType queryType,
            String message) {
        Instant now = clock.instant();
        AiMessage userMessage = messageRepository.save(
                AiMessage.user(
                        conversation.getId(),
                        queryType,
                        message,
                        now));
        updateConversationForMessage(conversation, message, now);
        return userMessage;
    }

    private AiSendMessageResult respondOutOfScope(
            Long userId,
            AiConversation conversation,
            AiMessage userMessage,
            AiQueryType queryType) {
        AiMessage assistantMessage = messageRepository.save(
                AiMessage.assistant(
                        conversation.getId(),
                        queryType,
                        OUT_OF_SCOPE_MESSAGE,
                        clock.instant()));
        requestLogRepository.save(AiRequestLog.rejected(
                userId,
                conversation.getId(),
                userMessage.getId(),
                queryType,
                properties.getPromptVersion(),
                clock.instant()));

        return result(
                conversation,
                userMessage,
                assistantMessage,
                false);
    }

    private AiConversation getOwned(
            Long userId,
            Long conversationId) {
        AiConversation conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(AiConversationNotFoundException::new);
        if (!conversation.belongsTo(userId)) {
            throw new AiConversationAccessException();
        }
        return conversation;
    }

    private String validateMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new AiRuleException("질문을 입력해 주세요.");
        }
        String normalized = message.trim();
        if (normalized.length() > properties.getMaxMessageLength()) {
            throw new AiRuleException(
                    "질문은 " + properties.getMaxMessageLength()
                            + "자 이하로 입력해 주세요.");
        }
        return normalized;
    }

    private void updateConversationForMessage(
            AiConversation conversation,
            String message,
            Instant now) {
        if (conversation.hasDefaultTitle()) {
            conversation.rename(titleFrom(message), now);
        } else {
            conversation.touch(now);
        }
        conversationRepository.save(conversation);
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

    private static AiSendMessageResult result(
            AiConversation conversation,
            AiMessage userMessage,
            AiMessage assistantMessage,
            boolean providerCalled) {
        return new AiSendMessageResult(
                AiConversationResult.from(conversation),
                AiMessageResult.from(userMessage),
                AiMessageResult.from(assistantMessage),
                providerCalled);
    }
}
