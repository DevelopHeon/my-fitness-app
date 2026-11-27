package com.myfitness.ai.application.service;

import com.myfitness.ai.application.usecase.response.AiUserMessageWriteResult;

import com.myfitness.ai.application.usecase.AiMessageTransactionUseCase;
import com.myfitness.ai.application.usecase.AiConversationUseCase;
import com.myfitness.ai.application.usecase.response.AiConversationResult;
import com.myfitness.ai.application.usecase.response.AiMessageResult;

import com.myfitness.ai.application.config.AiCoachProperties;
import com.myfitness.ai.application.port.out.AiChatGateway.AiModelResponse;
import com.myfitness.ai.domain.repository.AiConversationRepository;
import com.myfitness.ai.domain.repository.AiMessageRepository;
import com.myfitness.ai.application.port.out.AiPolicyGateway.AiPolicyAssessment;
import com.myfitness.ai.domain.repository.AiRequestLogRepository;
import com.myfitness.ai.application.support.context.AiContextBundle;
import com.myfitness.ai.application.support.policy.AiPolicyDecision.Action;
import com.myfitness.ai.application.support.policy.AiPolicyRun;
import com.myfitness.ai.domain.model.AiConversation;
import com.myfitness.ai.domain.model.AiMessage;
import com.myfitness.ai.domain.model.AiQueryType;
import com.myfitness.ai.domain.model.AiRequestLog;
import com.myfitness.ai.domain.model.AiRequestStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class AiMessageTransactionService implements AiMessageTransactionUseCase {
    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final AiRequestLogRepository requestLogRepository;
    private final AiConversationUseCase conversations;
    private final AiCoachProperties properties;
    private final Clock clock;

    @Autowired
    public AiMessageTransactionService(
            AiConversationRepository conversationRepository,
            AiMessageRepository messageRepository,
            AiRequestLogRepository requestLogRepository,
            AiConversationUseCase conversations,
            AiCoachProperties properties) {
        this(
                conversationRepository,
                messageRepository,
                requestLogRepository,
                conversations,
                properties,
                Clock.systemUTC());
    }

    AiMessageTransactionService(
            AiConversationRepository conversationRepository,
            AiMessageRepository messageRepository,
            AiRequestLogRepository requestLogRepository,
            AiConversationUseCase conversations,
            AiCoachProperties properties,
            Clock clock) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.requestLogRepository = requestLogRepository;
        this.conversations = conversations;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public List<AiMessage> loadMessagesForOwnedConversation(Long userId, Long conversationId) {
        conversations.requireOwned(userId, conversationId);
        return messageRepository.findAcceptedByConversationId(conversationId);
    }

    @Override
    @Transactional
    public AiUserMessageWriteResult saveUserMessage(Long userId, Long conversationId, String message) {
        AiConversation conversation = conversations.requireOwned(userId, conversationId);
        Instant now = clock.instant();

        AiMessage userMessage =
                messageRepository.save(
                        AiMessage.user(conversationId, AiQueryType.OUT_OF_SCOPE, message, now));

        if (conversation.hasDefaultTitle()) {
            conversation.rename(titleFrom(message), now);
        } else {
            conversation.touch(now);
        }
        conversationRepository.save(conversation);

        return new AiUserMessageWriteResult(AiConversationResult.from(conversation), AiMessageResult.from(userMessage, null));
    }

    @Override
    @Transactional
    public AiMessage saveRejectedResponse(
            Long userId,
            Long conversationId,
            AiMessageResult userMessage,
            AiQueryType queryType,
            String content,
            AiPolicyRun.Success policy) {
        AiMessage assistantMessage =
                messageRepository.save(
                        AiMessage.assistant(conversationId, queryType, content, clock.instant()));

        AiRequestLog log =
                AiRequestLog.rejected(
                        userId,
                        conversationId,
                        userMessage.id(),
                        queryType,
                        properties.getPromptVersion(),
                        clock.instant());
        AiRequestStatus status = rejectedStatus(policy);
        log.recordRejectedAssistant(assistantMessage.getId(), status);
        requestLogRepository.save(withPolicy(log, policy));

        return assistantMessage;
    }

    @Override
    @Transactional
    public AiMessage saveProviderSuccess(
            Long userId,
            Long conversationId,
            AiMessageResult userMessage,
            AiQueryType queryType,
            AiContextBundle context,
            AiModelResponse response,
            long latencyMs,
            AiPolicyRun.Success policy) {
        AiMessage assistantMessage =
                messageRepository.save(
                        AiMessage.assistant(
                                conversationId, queryType, response.content(), clock.instant()));

        requestLogRepository.save(
                withPolicy(
                        AiRequestLog.success(
                                userId,
                                conversationId,
                                userMessage.id(),
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
                                clock.instant()),
                        policy));

        return assistantMessage;
    }

    @Override
    @Transactional
    public void saveProviderFailure(
            Long userId,
            Long conversationId,
            AiMessageResult userMessage,
            AiQueryType queryType,
            AiContextBundle context,
            String provider,
            String model,
            RuntimeException exception,
            long latencyMs,
            AiPolicyRun.Success policy) {
        requestLogRepository.save(
                withPolicy(
                        AiRequestLog.failed(
                                userId,
                                conversationId,
                                userMessage.id(),
                                queryType,
                                provider,
                                model,
                                properties.getPromptVersion(),
                                latencyMs,
                                exception.getClass().getSimpleName(),
                                context.typeNames(),
                                clock.instant()),
                        policy));
    }

    @Override
    @Transactional
    public AiMessageResult classifyUserMessage(
            Long userId, Long conversationId, Long messageId, AiQueryType type) {
        conversations.requireOwned(userId, conversationId);
        AiMessage message = messageRepository.findByIdAndConversationId(messageId, conversationId).orElseThrow();
        message.classify(type);
        return AiMessageResult.from(messageRepository.save(message), null);
    }

    @Override
    @Transactional
    public void savePolicyFailure(
            Long userId, Long conversationId, AiMessageResult userMessage, AiPolicyRun.Failure failure) {
        conversations.requireOwned(userId, conversationId);
        AiRequestLog log =
                AiRequestLog.failed(
                        userId,
                        conversationId,
                        userMessage.id(),
                        AiQueryType.valueOf(userMessage.queryType()),
                        null,
                        null,
                        properties.getPromptVersion(),
                        0L,
                        "AI_POLICY_UNAVAILABLE",
                        null,
                        clock.instant());
        log.recordPolicy(
                failure.version(),
                null,
                null,
                null,
                failure.latencyMs(),
                null,
                failure.error().getLogCode(),
                null);
        requestLogRepository.save(log);
    }

    private AiRequestStatus rejectedStatus(AiPolicyRun.Success policy) {
        if (policy.decision().action() == Action.CLARIFY) {
            return AiRequestStatus.CLARIFICATION_REQUIRED;
        }
        if (policy.decision().reason().equals("OUT_OF_SCOPE")) {
            return AiRequestStatus.REJECTED_OUT_OF_SCOPE;
        }
        return AiRequestStatus.REJECTED_POLICY;
    }

    private AiRequestLog withPolicy(AiRequestLog log, AiPolicyRun.Success policy) {
        AiPolicyAssessment assessment = policy.assessment();
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put("medical_decision", assessment.medicalDecision());
        values.put("unsafe_action", assessment.unsafeAction());
        values.put("urgent_signal", assessment.urgentSignal());
        values.put("policy_bypass", assessment.policyBypass());
        values.put("topic", assessment.topic());
        String json = new ObjectMapper().writeValueAsString(values);
        log.recordPolicy(
                policy.version(),
                policy.decision().action().name(),
                policy.decision().reason(),
                assessment.model(),
                policy.latencyMs(),
                assessment.inputTokens(),
                null,
                json);
        return log;
    }

    private String titleFrom(String message) {
        String normalized = message.replaceAll("\\s+", " ").trim();
        int limit = Math.min(properties.getTitleMaxLength(), 100);
        if (normalized.length() <= limit) {
            return normalized;
        }
        return normalized.substring(0, limit);
    }
}
