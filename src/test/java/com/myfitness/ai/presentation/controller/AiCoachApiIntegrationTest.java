package com.myfitness.ai.presentation.controller;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static com.myfitness.test.security.TestSecurity.authenticatedUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myfitness.ai.application.port.out.AiChatGateway;
import com.myfitness.ai.application.port.out.AiPolicyGateway;
import com.myfitness.ai.application.port.out.AiChatGateway.AiModelRequest;
import com.myfitness.ai.application.port.out.AiChatGateway.AiModelResponse;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@Import(AiCoachApiIntegrationTest.FakeAiConfiguration.class)
class AiCoachApiIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired FakeAiChatGateway fakeGateway;
    @Autowired StubPolicyGateway policyGateway;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        jdbcTemplate.update("delete from ai_request_logs");
        jdbcTemplate.update("delete from ai_messages");
        jdbcTemplate.update("delete from ai_conversations");
        fakeGateway.reset();
        policyGateway.topic = "WORKOUT";
    }

    @Test
    @DisplayName("사용자는 여러 AI Conversation을 만들고 목록에서 조회할 수 있다")
    void createsAndListsMultipleConversations() throws Exception {
        createConversation(1L);
        createConversation(1L);

        mockMvc.perform(get("/api/ai/conversations")
                        .with(authenticatedUser(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("새 대화"));
    }

    @Test
    @DisplayName("앱 범위 밖 질문은 AI Provider를 호출하지 않고 안내 메시지를 저장한다")
    void rejectsOutOfScopeQuestionWithoutProviderCall() throws Exception {
        policyGateway.topic = "OUT_OF_SCOPE";
        long conversationId = createConversation(1L);

        mockMvc.perform(post(
                        "/api/ai/conversations/{id}/messages",
                        conversationId)
                        .with(authenticatedUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message":"자바 스프링 트랜잭션 설명해줘"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerCalled").value(false))
                .andExpect(jsonPath("$.assistantMessage.queryType")
                        .value("OUT_OF_SCOPE"))
                .andExpect(jsonPath("$.assistantMessage.content")
                        .value(org.hamcrest.Matchers.containsString(
                                "운동, 신체 기록, 식단 및 영양")));

        assertThat(fakeGateway.calls()).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "select status from ai_request_logs where conversation_id = ?",
                String.class,
                conversationId))
                .isEqualTo("REJECTED_OUT_OF_SCOPE");
    }

    @Test
    @DisplayName("Nutrition 질문은 개인 영양 Context와 함께 Provider를 호출하고 토큰 사용량을 저장한다")
    void sendsNutritionContextAndStoresProviderUsage() throws Exception {
        policyGateway.topic = "NUTRITION";
        long conversationId = createConversation(1L);

        mockMvc.perform(post(
                        "/api/ai/conversations/{id}/messages",
                        conversationId)
                        .with(authenticatedUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message":"오늘 식단 평가해줘",
                                  "clientContext":{
                                    "screen":"NUTRITION",
                                    "selectedDate":"2026-09-22"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerCalled").value(true))
                .andExpect(jsonPath("$.assistantMessage.content")
                        .value("fake personalized answer"));

        assertThat(fakeGateway.calls()).isEqualTo(1);
        assertThat(fakeGateway.lastRequest().context())
                .contains("[영양 기록 2026-09-22]")
                .contains("영양 목표 미등록");
        assertThat(jdbcTemplate.queryForObject(
                "select total_tokens from ai_request_logs where conversation_id = ?",
                Integer.class,
                conversationId))
                .isEqualTo(160);
        assertThat(jdbcTemplate.queryForObject(
                "select provider from ai_request_logs where conversation_id = ?",
                String.class,
                conversationId))
                .isEqualTo("fake");
    }

    @Test
    @DisplayName("후속 질문에는 같은 Conversation의 최근 User와 Assistant 메시지를 포함한다")
    void includesRecentConversationHistoryForFollowUp() throws Exception {
        long conversationId = createConversation(1L);

        send(conversationId, "운동 회복은 어떻게 해야 해?", 1L);
        send(conversationId, "조금 더 설명해줘", 1L);

        assertThat(fakeGateway.calls()).isEqualTo(2);
        assertThat(fakeGateway.lastRequest().history())
                .hasSize(2);
        assertThat(fakeGateway.lastRequest().history().get(0).content())
                .isEqualTo("운동 회복은 어떻게 해야 해?");
        assertThat(fakeGateway.lastRequest().history().get(1).content())
                .isEqualTo("fake personalized answer");
    }

    @Test
    @DisplayName("현재 질문과 다른 영역의 과거 대화는 Provider History에서 제외한다")
    void excludesUnrelatedConversationHistory() throws Exception {
        long conversationId = createConversation(1L);

        policyGateway.topic = "NUTRITION";
        send(conversationId, "오늘 식단 평가해줘", 1L);
        policyGateway.topic = "WORKOUT";
        send(conversationId, "오늘 운동 어떻게 할까?", 1L);

        assertThat(fakeGateway.calls()).isEqualTo(2);
        assertThat(fakeGateway.lastRequest().history()).isEmpty();
    }

    @Test
    @DisplayName("AI Provider 실패 시 사용자 메시지는 유지하고 실패 요청 로그를 남긴다")
    void storesFailedRequestLogWhenProviderFails() throws Exception {
        long conversationId = createConversation(1L);
        fakeGateway.failNext();

        mockMvc.perform(post(
                        "/api/ai/conversations/{id}/messages",
                        conversationId)
                        .with(authenticatedUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"오늘 운동 어떻게 할까?"}
                                """))
                .andExpect(status().isServiceUnavailable());

        assertThat(jdbcTemplate.queryForObject(
                "select status from ai_request_logs where conversation_id = ?",
                String.class,
                conversationId))
                .isEqualTo("FAILED");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from ai_messages where conversation_id = ?",
                Integer.class,
                conversationId))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("다른 사용자의 AI Conversation 메시지는 조회할 수 없다")
    void forbidsConversationAccessByAnotherUser() throws Exception {
        long conversationId = createConversation(1L);

        mockMvc.perform(get(
                        "/api/ai/conversations/{id}/messages",
                        conversationId)
                        .with(authenticatedUser(2L)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Conversation 삭제 시 메시지와 요청 로그도 함께 제거한다")
    void deletesConversationWithMessagesAndLogs() throws Exception {
        long conversationId = createConversation(1L);
        send(conversationId, "오늘 운동 어떻게 할까?", 1L);

        mockMvc.perform(delete(
                        "/api/ai/conversations/{id}",
                        conversationId)
                        .with(authenticatedUser(1L)))
                .andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from ai_conversations where id = ?",
                Integer.class,
                conversationId))
                .isZero();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from ai_messages where conversation_id = ?",
                Integer.class,
                conversationId))
                .isZero();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from ai_request_logs where conversation_id = ?",
                Integer.class,
                conversationId))
                .isZero();
    }

    private long createConversation(long userId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/ai/conversations")
                        .with(authenticatedUser(userId)))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).path("id").asLong();
    }

    private void send(
            long conversationId,
            String message,
            long userId) throws Exception {
        mockMvc.perform(post(
                        "/api/ai/conversations/{id}/messages",
                        conversationId)
                        .with(authenticatedUser(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"%s"}
                                """.formatted(message)))
                .andExpect(status().isOk());
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(
                result.getResponse().getContentAsString());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeAiConfiguration {
        @Bean
        @Primary
        StubPolicyGateway stubPolicyGateway() {
            return new StubPolicyGateway();
        }
        @Bean
        @Primary
        FakeAiChatGateway fakeAiChatGateway() {
            return new FakeAiChatGateway();
        }
    }

    static class StubPolicyGateway implements AiPolicyGateway {
        private String topic = "WORKOUT";
        @Override
        public AiPolicyAssessment assess(AiPolicyRequest request) {
            java.util.Map<String,Double> probabilities = TOPICS.stream().collect(
                    java.util.stream.Collectors.toMap(option -> option, option -> option.equals(topic) ? 1.0 : 0.0));
            return new AiPolicyAssessment("jev-1.13.0",0,0,0,0,
                    new TopicAssessment(topic,probabilities,1),1,1);
        }
    }
    static class FakeAiChatGateway implements AiChatGateway {
        private final AtomicInteger calls = new AtomicInteger();
        private AiModelRequest lastRequest;
        private boolean failNext;

        @Override
        public AiModelResponse chat(AiModelRequest request) {
            calls.incrementAndGet();
            lastRequest = request;
            if (failNext) {
                failNext = false;
                throw new IllegalStateException("fake provider failure");
            }
            return new AiModelResponse(
                    "fake personalized answer",
                    "fake",
                    "fake-model",
                    120,
                    40,
                    160);
        }

        @Override
        public String provider() {
            return "fake";
        }

        @Override
        public String model() {
            return "fake-model";
        }

        int calls() {
            return calls.get();
        }

        AiModelRequest lastRequest() {
            return lastRequest;
        }

        void failNext() {
            failNext = true;
        }

        void reset() {
            calls.set(0);
            lastRequest = null;
            failNext = false;
        }
    }
}
