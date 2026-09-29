package com.myfitness.ai.application.service;

import com.myfitness.ai.application.config.AiCoachProperties;
import com.myfitness.ai.application.exception.AiConversationAccessException;
import com.myfitness.ai.application.exception.AiConversationNotFoundException;
import com.myfitness.ai.application.port.out.AiChatGateway.AiModelResponse;
import com.myfitness.ai.application.port.out.AiConversationRepositoryPort;
import com.myfitness.ai.application.port.out.AiMessageRepositoryPort;
import com.myfitness.ai.application.port.out.AiPolicyGateway.AiPolicyAssessment;
import com.myfitness.ai.application.port.out.AiRequestLogRepositoryPort;
import com.myfitness.ai.application.support.context.AiContextBundle;
import com.myfitness.ai.application.support.policy.AiPolicyRun;
import com.myfitness.ai.domain.model.AiConversation;
import com.myfitness.ai.domain.model.AiMessage;
import com.myfitness.ai.domain.model.AiQueryType;
import com.myfitness.ai.domain.model.AiRequestLog;
import com.myfitness.ai.domain.model.AiRequestStatus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;

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

    public List<AiMessage> loadMessagesForOwnedConversation(Long userId, Long conversationId) {
        requireOwned(userId, conversationId);
        return messageRepository.findAcceptedByConversationId(conversationId);
    }

    @Transactional
    public UserMessageWrite saveUserMessage(Long userId, Long conversationId, String message) {
        AiConversation conversation = requireOwned(userId, conversationId);
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

        return new UserMessageWrite(conversation, userMessage);
    }

    @Transactional
    public AiMessage saveRejectedResponse(
            Long userId,
            Long conversationId,
            AiMessage userMessage,
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
                        userMessage.getId(),
                        queryType,
                        properties.getPromptVersion(),
                        clock.instant());
        AiRequestStatus status =
                policy.decision().action()
                                == com.myfitness.ai.application.support.policy.AiPolicyDecision.Action
                                        .CLARIFY
                        ? AiRequestStatus.CLARIFICATION_REQUIRED
                        : policy.decision().reason().equals("OUT_OF_SCOPE")
                                ? AiRequestStatus.REJECTED_OUT_OF_SCOPE
                                : AiRequestStatus.REJECTED_POLICY;
        log.recordRejectedAssistant(assistantMessage.getId(), status);
        requestLogRepository.save(withPolicy(log, policy));

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
                                clock.instant()),
                        policy));

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
            long latencyMs,
            AiPolicyRun.Success policy) {
        requestLogRepository.save(
                withPolicy(
                        AiRequestLog.failed(
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
                                clock.instant()),
                        policy));
    }

    @Transactional
    public AiMessage classifyUserMessage(
            Long userId, Long conversationId, AiMessage message, AiQueryType type) {
        requireOwned(userId, conversationId);
        message.classify(type);
        return messageRepository.save(message);
    }

    @Transactional
    public void savePolicyFailure(
            Long userId, Long conversationId, AiMessage userMessage, AiPolicyRun.Failure failure) {
        requireOwned(userId, conversationId);
        AiRequestLog log =
                AiRequestLog.failed(
                        userId,
                        conversationId,
                        userMessage.getId(),
                        userMessage.getQueryType(),
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
                failure.errorCode(),
                null);
        requestLogRepository.save(log);
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

    private AiConversation requireOwned(Long userId, Long conversationId) {
        AiConversation conversation =
                conversationRepository
                        .findById(conversationId)
                        .orElseThrow(AiConversationNotFoundException::new);
        if (!conversation.belongsTo(userId)) {
            throw new AiConversationAccessException();
        }
        return conversation;
    }

    private String titleFrom(String message) {
        String normalized = message.replaceAll("\\s+", " ").trim();
        int limit = Math.min(properties.getTitleMaxLength(), 100);
        if (normalized.length() <= limit) {
            return normalized;
        }
        return normalized.substring(0, limit);
    }

    public record UserMessageWrite(AiConversation conversation, AiMessage userMessage) {}
}
