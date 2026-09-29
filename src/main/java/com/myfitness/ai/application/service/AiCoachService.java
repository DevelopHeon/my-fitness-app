package com.myfitness.ai.application.service;

import com.myfitness.ai.application.config.AiCoachProperties;
import com.myfitness.ai.application.dto.request.AiClientContext;
import com.myfitness.ai.application.dto.request.AiMessageCommand;
import com.myfitness.ai.application.dto.response.AiConversationResult;
import com.myfitness.ai.application.dto.response.AiMessageResult;
import com.myfitness.ai.application.dto.response.AiSendMessageResult;
import com.myfitness.ai.application.exception.AiConversationAccessException;
import com.myfitness.ai.application.exception.AiConversationNotFoundException;
import com.myfitness.ai.application.exception.AiPolicyUnavailableException;
import com.myfitness.ai.application.exception.AiProviderUnavailableException;
import com.myfitness.ai.application.port.in.AiCoachUseCase;
import com.myfitness.ai.application.port.out.AiChatGateway.AiModelResponse;
import com.myfitness.ai.application.port.out.AiChatGateway.HistoryMessage;
import com.myfitness.ai.application.port.out.AiConversationRepositoryPort;
import com.myfitness.ai.application.port.out.AiMessageRepositoryPort;
import com.myfitness.ai.application.port.out.AiRequestLogRepositoryPort;
import com.myfitness.ai.application.support.AiHistorySelector;
import com.myfitness.ai.application.support.AiProviderExecutor;
import com.myfitness.ai.application.support.context.AiContextBuilder;
import com.myfitness.ai.application.support.context.AiContextBundle;
import com.myfitness.ai.application.support.policy.AiPolicyDecision;
import com.myfitness.ai.application.support.policy.AiPolicyGuard;
import com.myfitness.ai.application.support.policy.AiPolicyRun;
import com.myfitness.ai.domain.exception.AiRuleException;
import com.myfitness.ai.domain.model.AiConversation;
import com.myfitness.ai.domain.model.AiMessage;
import com.myfitness.ai.domain.model.AiQueryType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AiCoachService implements AiCoachUseCase {
    private static final String OUT_OF_SCOPE_MESSAGE =
            "My Fitness AI Coach에서는 운동, 신체 기록, 식단 및 영양과 관련된 질문을 도와드릴 수 있습니다.";

    private final AiConversationRepositoryPort conversationRepository;
    private final AiMessageRepositoryPort messageRepository;
    private final AiRequestLogRepositoryPort requestLogRepository;
    private final AiPolicyGuard policyGuard;
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
            AiPolicyGuard policyGuard,
            AiContextBuilder contextBuilder,
            AiHistorySelector historySelector,
            AiProviderExecutor providerExecutor,
            AiMessageTransactionService transactionService,
            AiCoachProperties properties) {
        this(
                conversationRepository,
                messageRepository,
                requestLogRepository,
                policyGuard,
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
            AiPolicyGuard policyGuard,
            AiContextBuilder contextBuilder,
            AiHistorySelector historySelector,
            AiProviderExecutor providerExecutor,
            AiMessageTransactionService transactionService,
            AiCoachProperties properties,
            Clock clock) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.requestLogRepository = requestLogRepository;
        this.policyGuard = policyGuard;
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
        AiConversation conversation =
                conversationRepository.save(AiConversation.create(userId, clock.instant()));
        return AiConversationResult.from(conversation);
    }

    @Override
    @Transactional
    public AiConversationResult renameConversation(Long userId, Long conversationId, String title) {
        AiConversation conversation = getOwned(userId, conversationId);
        conversation.rename(title, clock.instant());
        return AiConversationResult.from(conversationRepository.save(conversation));
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
    public List<AiMessageResult> listMessages(Long userId, Long conversationId) {
        getOwned(userId, conversationId);
        return messageRepository.findAllByConversationId(conversationId).stream()
                .map(AiMessageResult::from)
                .toList();
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public AiSendMessageResult sendMessage(
            Long userId, Long conversationId, AiMessageCommand command) {
        String message = validateMessage(command == null ? null : command.message());
        AiClientContext clientContext = command == null ? null : command.clientContext();

        List<AiMessage> previousMessages =
                transactionService.loadMessagesForOwnedConversation(userId, conversationId);
        AiMessageTransactionService.UserMessageWrite userWrite =
                transactionService.saveUserMessage(userId, conversationId, message);
        AiConversation conversation = userWrite.conversation();
        AiMessage userMessage = userWrite.userMessage();

        AiPolicyRun.Success policy =
                switch (policyGuard.evaluate(message, clientContext, previousMessages)) {
                    case AiPolicyRun.Success success -> success;
                    case AiPolicyRun.Failure failure -> {
                        transactionService.savePolicyFailure(
                                userId, conversationId, userMessage, failure);
                        throw new AiPolicyUnavailableException(failure.errorCode());
                    }
                };
        AiPolicyDecision decision = policy.decision();
        AiQueryType queryType = decision.storedQueryType();
        userMessage =
                transactionService.classifyUserMessage(
                        userId, conversationId, userMessage, queryType);
        if (decision.action() != AiPolicyDecision.Action.ALLOW) {
            AiMessage assistantMessage =
                    transactionService.saveRejectedResponse(
                            userId,
                            conversationId,
                            userMessage,
                            queryType,
                            policyMessage(decision),
                            policy);
            return result(
                    conversation, userMessage, assistantMessage, false, decision.action().name());
        }

        AiContextBundle context = contextBuilder.build(userId, queryType, clientContext, message);
        AiMessage assistantMessage =
                generateResponse(
                        userId,
                        conversationId,
                        userMessage,
                        queryType,
                        context,
                        historySelector.select(previousMessages, queryType),
                        message,
                        policy);

        return result(conversation, userMessage, assistantMessage, true, decision.action().name());
    }

    private AiMessage generateResponse(
            Long userId,
            Long conversationId,
            AiMessage userMessage,
            AiQueryType queryType,
            AiContextBundle context,
            List<HistoryMessage> history,
            String message,
            AiPolicyRun.Success policy) {
        long started = System.nanoTime();
        try {
            AiModelResponse response = providerExecutor.generate(context, history, message);
            return transactionService.saveProviderSuccess(
                    userId,
                    conversationId,
                    userMessage,
                    queryType,
                    context,
                    response,
                    elapsedMillis(started),
                    policy);
        } catch (RuntimeException exception) {
            transactionService.saveProviderFailure(
                    userId,
                    conversationId,
                    userMessage,
                    queryType,
                    context,
                    providerExecutor.provider(),
                    providerExecutor.model(),
                    exception,
                    elapsedMillis(started),
                    policy);
            if (exception instanceof AiProviderUnavailableException provider) {
                throw provider;
            }
            throw new AiProviderUnavailableException(
                    "AI 응답을 가져오지 못했습니다. 잠시 후 다시 시도해 주세요.", exception);
        }
    }

    private static long elapsedMillis(long started) {
        return (System.nanoTime() - started) / 1_000_000L;
    }

    private static String policyMessage(AiPolicyDecision decision) {
        return switch (decision.action()) {
            case SAFE_REDIRECT ->
                    "의료 진단, 치료·약물·질병 식단 처방이나 위험한 운동·식단 실행은 도와드릴 수 없습니다. 심한 통증, 실신 또는 심각한 부상이 있으면"
                        + " 운동을 계속하지 말고 전문 의료진의 확인을 받아 주세요.";
            case CLARIFY -> "어떤 운동, 신체 기록 또는 식단·영양에 대해 질문하시는지 조금 더 구체적으로 알려 주세요.";
            case BLOCK ->
                    decision.reason().equals("OUT_OF_SCOPE")
                            ? OUT_OF_SCOPE_MESSAGE
                            : "앱의 안전 규칙을 우회하거나 내부 지시를 공개하는 요청은 도와드릴 수 없습니다. 운동, 신체 기록, 식단·영양에 대해"
                                  + " 질문해 주세요.";
            case ALLOW -> throw new IllegalArgumentException("허용 질문에는 거절 안내를 만들지 않습니다.");
        };
    }

    private AiConversation getOwned(Long userId, Long conversationId) {
        AiConversation conversation =
                conversationRepository
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
            throw new AiRuleException("질문은 " + properties.getMaxMessageLength() + "자 이하로 입력해 주세요.");
        }
        return normalized;
    }

    private static AiSendMessageResult result(
            AiConversation conversation,
            AiMessage userMessage,
            AiMessage assistantMessage,
            boolean providerCalled,
            String policyDecision) {
        return new AiSendMessageResult(
                AiConversationResult.from(conversation),
                AiMessageResult.from(userMessage),
                AiMessageResult.from(assistantMessage),
                providerCalled,
                policyDecision);
    }
}
