package com.myfitness.ai.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myfitness.ai.domain.exception.AiRuleException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AiConversationTest {

    @Test
    @DisplayName("새 AI Conversation은 기본 제목과 사용자 소유권을 가진다")
    void createsConversationWithDefaultTitleAndOwner() {
        Instant now = Instant.parse("2026-09-22T03:00:00Z");

        AiConversation conversation =
                AiConversation.create(1L, now);

        assertThat(conversation.getTitle()).isEqualTo("새 대화");
        assertThat(conversation.belongsTo(1L)).isTrue();
        assertThat(conversation.belongsTo(2L)).isFalse();
        assertThat(conversation.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Conversation 제목 변경 시 제목과 수정 시각을 갱신한다")
    void renamesConversationAndUpdatesTimestamp() {
        AiConversation conversation = AiConversation.create(
                1L,
                Instant.parse("2026-09-22T03:00:00Z"));
        Instant changedAt =
                Instant.parse("2026-09-22T03:10:00Z");

        conversation.rename("최근 운동 분석", changedAt);

        assertThat(conversation.getTitle())
                .isEqualTo("최근 운동 분석");
        assertThat(conversation.getUpdatedAt())
                .isEqualTo(changedAt);
    }

    @Test
    @DisplayName("빈 Conversation 제목은 허용하지 않는다")
    void rejectsBlankConversationTitle() {
        AiConversation conversation = AiConversation.create(
                1L,
                Instant.parse("2026-09-22T03:00:00Z"));

        assertThatThrownBy(() ->
                conversation.rename(
                        " ",
                        Instant.parse("2026-09-22T03:10:00Z")))
                .isInstanceOf(AiRuleException.class);
    }
}
