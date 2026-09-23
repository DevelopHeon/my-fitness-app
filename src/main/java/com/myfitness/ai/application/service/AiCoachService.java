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
import java.time.Clock;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
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
    private final AiMessageTransactionService transactionService;
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
            AiMessageTransactionService transactionService,
            AiCoachProperties properties) {
        this(
                conversationRepository,
                messageRepository,
                requestLogRepository,
                queryRouter,
                contextBuilder,
                historySelector,
                providerExecutor,
                transactionService,
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
            AiMessageTransactionService transactionService,
            AiCoachProperties properties,
            Clock clock) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.requestLogRepository = requestLogRepository;
        this.queryRouter = queryRouter;
        this.contextBuilder = contextBuilder;
        this.historySelector = historySelector;
        this.providerExecutor = providerExecutor;
        this.transactionService = transactionService;
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
    @Transactional
    public AiConversationResult createConversation(Long userId) {
        AiConversation conversation = conversationRepository.save(
                AiConversation.create(userId, clock.instant()));
        return AiConversationResult.from(conversation);
    }

    @Override
    @Transactional
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
    @Transactional
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
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AiSendMessageResult sendMessage(
            Long userId,
            Long conversationId,
            AiMessageCommand command) {
        String message = validateMessage(
                command == null ? null : command.message());
        AiClientContext clientContext =
                command == null ? null : command.clientContext();

        List<AiMessage> previousMessages =
                transactionService.loadMessagesForOwnedConversation(
                        userId,
                        conversationId);
        AiQueryType queryType = queryRouter.route(
                message,
                clientContext,
                historySelector.latestUserQueryType(previousMessages));

        AiMessageTransactionService.UserMessageWrite userWrite =
                transactionService.saveUserMessage(
                        userId,
                        conversationId,
                        queryType,
                        message);
        AiConversation conversation = userWrite.conversation();
        AiMessage userMessage = userWrite.userMessage();

        if (queryType == AiQueryType.OUT_OF_SCOPE) {
            AiMessage assistantMessage =
                    transactionService.saveRejectedResponse(
                            userId,
                            conversationId,
                            userMessage,
                            queryType,
                            OUT_OF_SCOPE_MESSAGE);
            return result(
                    conversation,
                    userMessage,
                    assistantMessage,
                    false);
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
