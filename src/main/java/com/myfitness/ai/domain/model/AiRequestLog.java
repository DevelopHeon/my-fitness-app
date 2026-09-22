package com.myfitness.ai.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ai_request_logs")
public class AiRequestLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "user_message_id", nullable = false)
    private Long userMessageId;

    @Column(name = "assistant_message_id")
    private Long assistantMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "query_type", nullable = false, length = 30)
    private AiQueryType queryType;

    @Column(length = 40)
    private String provider;

    @Column(length = 100)
    private String model;

    @Column(name = "prompt_version", nullable = false, length = 50)
    private String promptVersion;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiRequestStatus status;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "context_types", length = 500)
    private String contextTypes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AiRequestLog() {}

    private AiRequestLog(
            Long userId,
            Long conversationId,
            Long userMessageId,
            Long assistantMessageId,
            AiQueryType queryType,
            String provider,
            String model,
            String promptVersion,
            Integer inputTokens,
            Integer outputTokens,
            Integer totalTokens,
            Long latencyMs,
            AiRequestStatus status,
            String errorCode,
            String contextTypes,
            Instant createdAt) {
        this.userId = userId;
        this.conversationId = conversationId;
        this.userMessageId = userMessageId;
        this.assistantMessageId = assistantMessageId;
        this.queryType = queryType;
        this.provider = provider;
        this.model = model;
        this.promptVersion = promptVersion;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.totalTokens = totalTokens;
        this.latencyMs = latencyMs;
        this.status = status;
        this.errorCode = errorCode;
        this.contextTypes = contextTypes;
        this.createdAt = createdAt;
    }

    public static AiRequestLog success(
            Long userId,
            Long conversationId,
            Long userMessageId,
            Long assistantMessageId,
            AiQueryType queryType,
            String provider,
            String model,
            String promptVersion,
            Integer inputTokens,
            Integer outputTokens,
            Integer totalTokens,
            Long latencyMs,
            String contextTypes,
            Instant createdAt) {
        return new AiRequestLog(
                userId, conversationId, userMessageId, assistantMessageId,
                queryType, provider, model, promptVersion,
                inputTokens, outputTokens, totalTokens, latencyMs,
                AiRequestStatus.SUCCESS, null, contextTypes, createdAt);
    }

    public static AiRequestLog rejected(
            Long userId,
            Long conversationId,
            Long userMessageId,
            AiQueryType queryType,
            String promptVersion,
            Instant createdAt) {
        return new AiRequestLog(
                userId, conversationId, userMessageId, null,
                queryType, null, null, promptVersion,
                null, null, null, 0L,
                AiRequestStatus.REJECTED_OUT_OF_SCOPE,
                null, null, createdAt);
    }

    public static AiRequestLog failed(
            Long userId,
            Long conversationId,
            Long userMessageId,
            AiQueryType queryType,
            String provider,
            String model,
            String promptVersion,
            Long latencyMs,
            String errorCode,
            String contextTypes,
            Instant createdAt) {
        return new AiRequestLog(
                userId, conversationId, userMessageId, null,
                queryType, provider, model, promptVersion,
                null, null, null, latencyMs,
                AiRequestStatus.FAILED, errorCode, contextTypes, createdAt);
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getConversationId() { return conversationId; }
    public Long getUserMessageId() { return userMessageId; }
    public Long getAssistantMessageId() { return assistantMessageId; }
    public AiQueryType getQueryType() { return queryType; }
    public String getProvider() { return provider; }
    public String getModel() { return model; }
    public String getPromptVersion() { return promptVersion; }
    public Integer getInputTokens() { return inputTokens; }
    public Integer getOutputTokens() { return outputTokens; }
    public Integer getTotalTokens() { return totalTokens; }
    public Long getLatencyMs() { return latencyMs; }
    public AiRequestStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
    public String getContextTypes() { return contextTypes; }
    public Instant getCreatedAt() { return createdAt; }
}
