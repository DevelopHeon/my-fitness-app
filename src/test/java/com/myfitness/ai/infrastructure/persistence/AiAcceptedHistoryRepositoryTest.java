package com.myfitness.ai.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.myfitness.ai.domain.model.AiConversation;
import com.myfitness.ai.domain.model.AiMessage;
import com.myfitness.ai.domain.model.AiQueryType;
import com.myfitness.ai.domain.model.AiRequestLog;
import com.myfitness.ai.domain.model.AiRequestStatus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@SpringBootTest
@Transactional
class AiAcceptedHistoryRepositoryTest {
    @Autowired SpringDataAiConversationRepository conversations;
    @Autowired SpringDataAiMessageRepository messages;
    @Autowired SpringDataAiRequestLogRepository logs;
    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("겹친 요청의 USER USER USER ASSISTANT 이력을 요청 로그의 정확한 쌍으로 조회한다")
    void preservesLinkedPairsForConcurrentRequests() {
        Instant now = Instant.parse("2026-09-28T00:00:00Z");
        long id = conversations.saveAndFlush(AiConversation.create(1L, now)).getId();
        AiMessage u1 = messages.saveAndFlush(AiMessage.user(id, AiQueryType.WORKOUT, "질문1", now));
        AiMessage u2 =
                messages.saveAndFlush(
                        AiMessage.user(id, AiQueryType.WORKOUT, "질문2", now.plusSeconds(1)));
        AiMessage u3 =
                messages.saveAndFlush(
                        AiMessage.user(id, AiQueryType.WORKOUT, "질문3", now.plusSeconds(2)));
        AiMessage a1 =
                messages.saveAndFlush(
                        AiMessage.assistant(id, AiQueryType.WORKOUT, "답변1", now.plusSeconds(3)));
        AiMessage a2 =
                messages.saveAndFlush(
                        AiMessage.assistant(id, AiQueryType.WORKOUT, "답변2", now.plusSeconds(4)));
        AiMessage a3 =
                messages.saveAndFlush(
                        AiMessage.assistant(id, AiQueryType.WORKOUT, "답변3", now.plusSeconds(5)));
        logs.saveAndFlush(
                success(
                        id, u1, a1,
                        now)); // Historical SUCCESS+null remains accepted; old shadow modes do not
                               // select a runtime path.
        AiRequestLog second = success(id, u2, a2, now);
        second.recordPolicy("v1", "ALLOW", "ALLOWED", "jev-1.13.0", 1, 1, null, null);
        logs.saveAndFlush(second);
        jdbc.update("update ai_request_logs set policy_mode='shadow' where id=?", second.getId());
        logs.saveAndFlush(success(id, u3, a3, now));
        assertThat(messages.findAcceptedByConversationId(id, AiRequestStatus.SUCCESS))
                .extracting(AiMessage::getContent)
                .containsExactly("질문1", "답변1", "질문2", "답변2", "질문3", "답변3");
    }

    private AiRequestLog success(long id, AiMessage user, AiMessage assistant, Instant now) {
        return AiRequestLog.success(
                1L,
                id,
                user.getId(),
                assistant.getId(),
                AiQueryType.WORKOUT,
                "fake",
                "fake",
                "v1",
                1,
                1,
                2,
                1L,
                "",
                now);
    }
}
