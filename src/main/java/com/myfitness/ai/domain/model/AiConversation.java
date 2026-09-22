package com.myfitness.ai.domain.model;

import com.myfitness.ai.domain.exception.AiRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ai_conversations")
public class AiConversation {
    private static final String DEFAULT_TITLE = "새 대화";
    private static final int MAX_TITLE_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = MAX_TITLE_LENGTH)
    private String title;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AiConversation() {}

    private AiConversation(Long userId, Instant now) {
        validateUserId(userId);
        if (now == null) {
            throw new AiRuleException("Conversation 생성 시각이 필요합니다.");
        }
        this.userId = userId;
        this.title = DEFAULT_TITLE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static AiConversation create(Long userId, Instant now) {
        return new AiConversation(userId, now);
    }

    public void rename(String title, Instant now) {
        if (title == null || title.isBlank()) {
            throw new AiRuleException("대화 제목은 비어 있을 수 없습니다.");
        }
        String normalized = title.trim();
        if (normalized.length() > MAX_TITLE_LENGTH) {
            throw new AiRuleException("대화 제목은 100자 이하이어야 합니다.");
        }
        this.title = normalized;
        this.updatedAt = requireNow(now);
    }

    public void touch(Instant now) {
        this.updatedAt = requireNow(now);
    }

    public boolean belongsTo(Long userId) {
        return this.userId.equals(userId);
    }

    public boolean hasDefaultTitle() {
        return DEFAULT_TITLE.equals(title);
    }

    private static void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new AiRuleException("유효한 사용자 ID가 필요합니다.");
        }
    }

    private static Instant requireNow(Instant now) {
        if (now == null) {
            throw new AiRuleException("변경 시각이 필요합니다.");
        }
        return now;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTitle() { return title; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
