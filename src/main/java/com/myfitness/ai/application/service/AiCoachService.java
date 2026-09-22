package com.myfitness.ai.application.service;

import com.myfitness.ai.application.command.AiMessageCommand;
import com.myfitness.ai.application.config.AiCoachProperties;
import com.myfitness.ai.application.context.AiContextBuilder;
import com.myfitness.ai.application.context.AiContextBundle;
import com.myfitness.ai.application.exception.AiConversationAccessException;
import com.myfitness.ai.application.exception.AiConversationNotFoundException;
import com.myfitness.ai.application.exception.AiProviderUnavailableException;
import com.myfitness.ai.application.port.in.AiCoachUseCase;
import com.myfitness.ai.application.port.out.AiChatGateway;
import com.myfitness.ai.application.port.out.AiChatGateway.AiModelRequest;
import com.myfitness.ai.application.port.out.AiChatGateway.AiModelResponse;
import com.myfitness.ai.application.port.out.AiChatGateway.HistoryMessage;
import com.myfitness.ai.application.port.out.AiConversationRepositoryPort;
import com.myfitness.ai.application.port.out.AiMessageRepositoryPort;
import com.myfitness.ai.application.port.out.AiRequestLogRepositoryPort;
import com.myfitness.ai.application.prompt.AiSystemPrompt;
import com.myfitness.ai.application.result.AiConversationResult;
import com.myfitness.ai.application.result.AiMessageResult;
import com.myfitness.ai.application.result.AiSendMessageResult;
import com.myfitness.ai.application.router.AiQueryRouter;
import com.myfitness.ai.domain.exception.AiRuleException;
import com.myfitness.ai.domain.model.AiConversation;
import com.myfitness.ai.domain.model.AiMessage;
import com.myfitness.ai.domain.model.AiMessageRole;
import com.myfitness.ai.domain.model.AiQueryType;
import com.myfitness.ai.domain.model.AiRequestLog;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
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
    private final AiChatGateway chatGateway;
    private final AiQueryRouter queryRouter;
    private final AiContextBuilder contextBuilder;
    private final AiCoachProperties properties;
    private final Clock clock;

    @Autowired
    public AiCoachService(
            AiConversationRepositoryPort conversationRepository,
            AiMessageRepositoryPort messageRepository,
            AiRequestLogRepositoryPort requestLogRepository,
            AiChatGateway chatGateway,
            AiQueryRouter queryRouter,
            AiContextBuilder contextBuilder,
            AiCoachProperties properties) {
        this(
                conversationRepository,
                messageRepository,
                requestLogRepository,
                chatGateway,
                queryRouter,
                contextBuilder,
                properties,
                Clock.systemUTC());
    }

    AiCoachService(
            AiConversationRepositoryPort conversationRepository,
            AiMessageRepositoryPort messageRepository,
            AiRequestLogRepositoryPort requestLogRepository,
            AiChatGateway chatGateway,
            AiQueryRouter queryRouter,
            AiContextBuilder contextBuilder,
            AiCoachProperties properties,
            Clock clock) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.requestLogRepository = requestLogRepository;
        this.chatGateway = chatGateway;
        this.queryRouter = queryRouter;
        this.contextBuilder = contextBuilder;
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
        String message = validateMessage(command == null ? null : command.message());

        List<AiMessage> previousMessages =
                messageRepository.findAllByConversationId(conversationId);
        AiQueryType previousType = previousMessages.stream()
                .filter(item -> item.getRole() == AiMessageRole.USER)
                .map(AiMessage::getQueryType)
                .reduce((first, second) -> second)
                .orElse(null);

        AiQueryType queryType = queryRouter.route(
                message,
                command.clientContext(),
                previousType);

        Instant now = clock.instant();
        AiMessage userMessage = messageRepository.save(
                AiMessage.user(
                        conversationId,
                        queryType,
                        message,
                        now));
        updateConversationForMessage(conversation, message, now);

        if (queryType == AiQueryType.OUT_OF_SCOPE) {
            AiMessage assistantMessage = messageRepository.save(
                    AiMessage.assistant(
                            conversationId,
                            queryType,
                            OUT_OF_SCOPE_MESSAGE,
                            clock.instant()));
            requestLogRepository.save(AiRequestLog.rejected(
                    userId,
                    conversationId,
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

        AiContextBundle context = contextBuilder.build(
                userId,
                queryType,
                command.clientContext(),
                message);
        List<HistoryMessage> history =
                selectHistory(previousMessages, queryType);

        long started = System.nanoTime();
        try {
            AiModelResponse response = chatGateway.chat(
                    new AiModelRequest(
                            AiSystemPrompt.create(
                                    properties.getPromptVersion()),
                            context.text(),
                            history,
                            message));
            long latencyMs = elapsedMillis(started);

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

            return result(
                    conversation,
                    userMessage,
                    assistantMessage,
                    true);
        } catch (RuntimeException exception) {
            requestLogRepository.save(AiRequestLog.failed(
                    userId,
                    conversationId,
                    userMessage.getId(),
                    queryType,
                    chatGateway.provider(),
                    chatGateway.model(),
                    properties.getPromptVersion(),
                    elapsedMillis(started),
                    exception.getClass().getSimpleName(),
                    context.typeNames(),
                    clock.instant()));

            if (exception instanceof AiProviderUnavailableException providerException) {
                throw providerException;
            }
            throw new AiProviderUnavailableException(
                    "AI 응답을 가져오지 못했습니다. 잠시 후 다시 시도해 주세요.",
                    exception);
        }
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

    private List<HistoryMessage> selectHistory(
            List<AiMessage> messages,
            AiQueryType currentType) {
        int messageLimit = Math.max(
                0,
                properties.getHistoryMessageLimit());
        int charLimit = Math.max(
                0,
                properties.getHistoryCharLimit());

        List<HistoryMessage> reversed = new ArrayList<>();
        int chars = 0;

        for (int index = messages.size() - 1;
                index >= 0 && reversed.size() < messageLimit;
                index--) {
            AiMessage message = messages.get(index);
            if (!isRelevantHistory(message.getQueryType(), currentType)) {
                continue;
            }
            if (chars >= charLimit) {
                break;
            }
            int remaining = charLimit - chars;
            String content = message.getContent();
            if (content.length() > remaining) {
                content = content.substring(
                        Math.max(0, content.length() - remaining));
            }
            reversed.add(new HistoryMessage(
                    message.getRole(),
                    content));
            chars += content.length();
        }

        List<HistoryMessage> result = new ArrayList<>();
        for (int index = reversed.size() - 1; index >= 0; index--) {
            result.add(reversed.get(index));
        }
        return List.copyOf(result);
    }

    private static boolean isRelevantHistory(
            AiQueryType historyType,
            AiQueryType currentType) {
        if (historyType == null
                || historyType == AiQueryType.OUT_OF_SCOPE) {
            return false;
        }
        if (currentType == AiQueryType.COMPOSITE
                || currentType == AiQueryType.GENERAL_FITNESS) {
            return true;
        }
        return historyType == currentType
                || historyType == AiQueryType.COMPOSITE;
    }

    private static long elapsedMillis(long started) {
        return (System.nanoTime() - started) / 1_000_000L;
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
