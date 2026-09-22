package com.myfitness.ai.domain.model;

import com.myfitness.ai.domain.exception.AiRuleException;
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
@Table(name = "ai_messages")
public class AiMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiMessageRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "query_type", nullable = false, length = 30)
    private AiQueryType queryType;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AiMessage() {}

    private AiMessage(
            Long conversationId,
            AiMessageRole role,
            AiQueryType queryType,
            String content,
            Instant createdAt) {
        if (conversationId == null || conversationId <= 0) {
            throw new AiRuleException("Conversation ID가 필요합니다.");
        }
        if (role == null || queryType == null) {
            throw new AiRuleException("메시지 역할과 질문 유형이 필요합니다.");
        }
        if (content == null || content.isBlank()) {
            throw new AiRuleException("메시지 내용은 비어 있을 수 없습니다.");
        }
        if (createdAt == null) {
            throw new AiRuleException("메시지 생성 시각이 필요합니다.");
        }
        this.conversationId = conversationId;
        this.role = role;
        this.queryType = queryType;
        this.content = content.trim();
        this.createdAt = createdAt;
    }

    public static AiMessage user(
            Long conversationId,
            AiQueryType queryType,
            String content,
            Instant createdAt) {
        return new AiMessage(
                conversationId,
                AiMessageRole.USER,
                queryType,
                content,
                createdAt);
    }

    public static AiMessage assistant(
            Long conversationId,
            AiQueryType queryType,
            String content,
            Instant createdAt) {
        return new AiMessage(
                conversationId,
                AiMessageRole.ASSISTANT,
                queryType,
                content,
                createdAt);
    }

    public Long getId() { return id; }
    public Long getConversationId() { return conversationId; }
    public AiMessageRole getRole() { return role; }
    public AiQueryType getQueryType() { return queryType; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
