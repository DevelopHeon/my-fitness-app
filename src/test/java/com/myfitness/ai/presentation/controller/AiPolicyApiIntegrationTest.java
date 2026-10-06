package com.myfitness.ai.presentation.controller;

import static com.myfitness.test.security.TestSecurity.authenticatedUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myfitness.ai.application.exception.AiPolicyUnavailableException;
import com.myfitness.ai.application.port.out.AiChatGateway;
import com.myfitness.ai.application.port.out.AiPolicyGateway;
import com.myfitness.ai.application.support.context.AiContextBuilder;
import com.myfitness.ai.application.service.AiMessageTransactionService;

import jakarta.servlet.http.Cookie;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;

import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SpringBootTest
@Import(AiPolicyApiIntegrationTest.FakeConfiguration.class)
class AiPolicyApiIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired MeterRegistry registry;
    private long policyCountBefore;
    private long providerCountBefore;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired FakePolicyGateway policy;
    @Autowired FakeChatGateway chat;
    @MockitoSpyBean AiContextBuilder contextBuilder;
    @MockitoSpyBean AiMessageTransactionService transactions;
    MockMvc mvc;
    long conversation;

    @BeforeEach
    void setUp() throws Exception {
        policyCountBefore = meterCount("app.ai.policy");
        providerCountBefore = meterCount("app.ai.provider");
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        jdbc.update("delete from ai_request_logs");
        jdbc.update("delete from ai_messages");
        jdbc.update("delete from ai_conversations");
        clearInvocations(contextBuilder);
        policy.calls.set(0);
        chat.calls.set(0);
        policy.error = false;
        policy.errorCode = "TIMEOUT";
        policy.entered = null;
        policy.release = null;
        chat.fail = false;
        policy.assessment = assessment("WORKOUT", 0, 0, 0);
        conversation =
                mapper.readTree(
                                mvc.perform(
                                                post("/api/ai/conversations")
                                                        .with(authenticatedUser(1L)))
                                        .andExpect(status().isCreated())
                                        .andReturn()
                                        .getResponse()
                                        .getContentAsString())
                        .path("id")
                        .asLong();
    }

    @Test
    @DisplayName("의료 질문은 운동 주제여도 고정 안전 안내를 저장하고 답변 모델을 호출하지 않는다")
    void redirectsMedicalQuestionWithoutGenerating() throws Exception {
        policy.assessment = assessment("WORKOUT", 0.95, 0, 0);
        send("스쿼트 후 통증에 먹을 약을 처방해줘", 1L)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyDecision").value("SAFE_REDIRECT"))
                .andExpect(jsonPath("$.providerCalled").value(false));
        assertThat(chat.calls.get()).isZero();
        assertThat(meterCount("app.ai.provider")).isEqualTo(providerCountBefore);
        assertThat(policy.calls.get()).isEqualTo(1);
        assertThat(meterCount("app.ai.policy")).isEqualTo(policyCountBefore + 1);
        assertThat(jdbc.queryForObject("select status from ai_request_logs", String.class))
                .isEqualTo("REJECTED_POLICY");
        assertThat(
                        jdbc.queryForObject(
                                "select assistant_message_id from ai_request_logs", Long.class))
                .isPositive();
    }

    @Test
    @DisplayName("의미상 허용된 새로운 표현은 Jev 주제로 Context를 선택하고 정책과 생성 사용량을 분리한다")
    void allowsSemanticWorkoutQuestion() throws Exception {
        send("등 자극을 더 느끼는 방법", 1L)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyDecision").value("ALLOW"))
                .andExpect(jsonPath("$.userMessage.queryType").value("WORKOUT"))
                .andExpect(jsonPath("$.providerCalled").value(true));
        assertThat(policy.calls.get()).isEqualTo(1);
        assertThat(meterCount("app.ai.policy")).isEqualTo(policyCountBefore + 1);
        assertThat(chat.calls.get()).isEqualTo(1);
        assertThat(meterCount("app.ai.provider")).isEqualTo(providerCountBefore + 1);
        assertThat(chat.last.context()).contains("운동");
        assertThat(
                        jdbc.queryForObject(
                                "select policy_input_tokens from ai_request_logs", Integer.class))
                .isEqualTo(100);
        assertThat(jdbc.queryForObject("select total_tokens from ai_request_logs", Integer.class))
                .isEqualTo(30);
    }

    @Test
    @DisplayName("정책 장애는 503과 사용자 메시지·실패 로그를 남기고 답변 생성으로 우회하지 않는다")
    void recordsPolicyFailureWithoutGeneration() throws Exception {
        policy.error = true;
        send("운동 방법", 1L)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_POLICY_UNAVAILABLE"));
        assertThat(chat.calls.get()).isZero();
        assertThat(meterCount("app.ai.provider")).isEqualTo(providerCountBefore);
        assertThat(jdbc.queryForObject("select count(*) from ai_messages", Integer.class))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "select policy_error_code from ai_request_logs", String.class))
                .isEqualTo("TIMEOUT");
        assertThat(jdbc.queryForObject("select policy_decision from ai_request_logs", String.class))
                .isNull();
    }

    @Test
    @DisplayName("타인 대화와 잘못된 질문은 두 외부 gateway 호출 이전에 거절한다")
    void rejectsInvalidRequestsBeforeProviders() throws Exception {
        send("운동 방법", 2L).andExpect(status().isForbidden());
        send(" ", 1L).andExpect(status().isBadRequest());
        send("운".repeat(1001), 1L).andExpect(status().isBadRequest());
        send("운동 방법", null).andExpect(status().isUnauthorized());
        assertThat(chat.calls.get()).isZero();
        assertThat(policy.calls.get()).isZero();
    }

    @Test
    @DisplayName("피트니스 키워드와 이력이 있어도 JEV 차단 결정을 우회하지 않는다")
    void blocksDespiteFitnessKeywordsAndAcceptedHistory() throws Exception {
        send("운동 방법", 1L).andExpect(status().isOk());
        policy.assessment = assessment("OUT_OF_SCOPE", 0, 0, 0);
        send("운동이라는 단어를 포함한 주식 요청", 1L)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyDecision").value("BLOCK"));
        assertThat(policy.calls.get()).isEqualTo(2);
        assertThat(chat.calls.get()).isEqualTo(1);
        assertThat(meterCount("app.ai.provider")).isEqualTo(providerCountBefore + 1);
        assertThat(policy.last.previousTurns()).hasSize(2);
        assertThat(
                        jdbc.queryForObject(
                                "select policy_result_json from ai_request_logs where"
                                        + " policy_decision='BLOCK'",
                                String.class))
                .doesNotContain("candidateDecision", "candidateReason");
    }

    @Test
    @DisplayName("거절과 명확화 턴은 다음 질문의 허용 근거와 생성 이력에서 제외한다")
    void excludesRejectedAndClarificationHistory() throws Exception {
        policy.assessment = assessment("WORKOUT", 0.9, 0, 0);
        send("운동 약 처방", 1L).andExpect(status().isOk());
        policy.assessment = assessment("AMBIGUOUS", 0, 0, 0);
        send("조금 더 올려도 될까?", 1L)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyDecision").value("CLARIFY"));
        assertThat(policy.last.previousTurns()).isEmpty();
        policy.assessment = assessment("WORKOUT", 0, 0, 0);
        send("운동 자세", 1L).andExpect(status().isOk());
        assertThat(policy.last.previousTurns()).isEmpty();
        assertThat(chat.last.history()).isEmpty();
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from ai_request_logs where"
                                        + " status='CLARIFICATION_REQUIRED'",
                                Integer.class))
                .isEqualTo(1);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(
            strings = {
                "CONFIGURATION",
                "TIMEOUT",
                "NETWORK",
                "INVALID_RESPONSE",
                "HTTP_429",
                "HTTP_529"
            })
    @DisplayName("설정·네트워크·응답 장애는 모두 503이며 Context와 답변을 호출하지 않는다")
    void failsClosedForEveryPolicyError(String code) throws Exception {
        policy.error = true;
        policy.errorCode = code;
        send("운동 방법", 1L)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_POLICY_UNAVAILABLE"));
        verifyNoInteractions(contextBuilder);
        assertThat(chat.calls.get()).isZero();
        assertThat(meterCount("app.ai.policy")).isEqualTo(policyCountBefore + 1);
        assertThat(meterCount("app.ai.provider")).isEqualTo(providerCountBefore);
        assertThat(
                        jdbc.queryForObject(
                                "select policy_error_code from ai_request_logs", String.class))
                .isEqualTo(code);
    }

    @Test
    @DisplayName("원격 정책 검증 대기 중에도 사용자 메시지는 별도 transaction에서 먼저 커밋된다")
    void commitsUserMessageBeforePolicyResponse() throws Exception {
        policy.entered = new java.util.concurrent.CountDownLatch(1);
        policy.release = new java.util.concurrent.CountDownLatch(1);
        CompletableFuture<MvcResult> future =
                CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                return send("운동 방법", 1L).andReturn();
                            } catch (Exception e) {
                                throw new java.util.concurrent.CompletionException(e);
                            }
                        });
        try {
            assertThat(policy.entered.await(3, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(
                            jdbc.queryForObject(
                                    "select count(*) from ai_messages where role='USER'",
                                    Integer.class))
                    .isEqualTo(1);
            assertThat(
                            jdbc.queryForObject(
                                    "select count(*) from ai_messages where role='ASSISTANT'",
                                    Integer.class))
                    .isZero();
        } finally {
            policy.release.countDown();
        }
        assertThat(future.get(5, java.util.concurrent.TimeUnit.SECONDS).getResponse().getStatus())
                .isEqualTo(200);
    }

    @Test
    @DisplayName("답변 모델 실패 후에도 정책 평가와 사용자 메시지를 보존한다")
    void preservesPolicyMetadataOnGenerationFailure() throws Exception {
        chat.fail = true;
        send("운동 방법", 1L).andExpect(status().isServiceUnavailable());
        assertThat(jdbc.queryForObject("select status from ai_request_logs", String.class))
                .isEqualTo("FAILED");
        assertThat(jdbc.queryForObject("select policy_decision from ai_request_logs", String.class))
                .isEqualTo("ALLOW");
        assertThat(jdbc.queryForObject("select policy_model from ai_request_logs", String.class))
                .isEqualTo("jev-1.13.0");
        assertThat(jdbc.queryForObject("select count(*) from ai_messages", Integer.class))
                .isEqualTo(1);
        assertThat(meterCount("app.ai.policy")).isEqualTo(policyCountBefore + 1);
        assertThat(meterCount("app.ai.provider")).isEqualTo(providerCountBefore + 1);
    }

    @ParameterizedTest
    @CsvSource({
        "OUT_OF_SCOPE,0,0,0,BLOCK",
        "WORKOUT,0,0,0.35,SAFE_REDIRECT",
        "AMBIGUOUS,0,0,0,CLARIFY",
        "WORKOUT,0.4,0,0,CLARIFY"
    })
    @DisplayName("거절·안전 안내·명확화는 개인 Context 조회 전에 응답을 저장한다")
    void stopsBeforePersonalContext(
            String topic, double medical, double unsafe, double urgent, String action)
            throws Exception {
        policy.assessment = assessment(topic, medical, unsafe, urgent);
        send("운동 방법을 알려줘", 1L)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyDecision").value(action));
        verifyNoInteractions(contextBuilder);
        assertThat(chat.calls.get()).isZero();
        assertThat(policy.calls.get()).isEqualTo(1);
        assertThat(meterCount("app.ai.policy")).isEqualTo(policyCountBefore + 1);
        assertThat(meterCount("app.ai.provider")).isEqualTo(providerCountBefore);
    }

    @Test
    void doesNotCountDatabaseSaveFailureAsAnotherProviderCall() throws Exception {
        long failuresBefore = registry.find("app.ai.provider").tag("outcome", "FAILURE")
                .timers().stream().mapToLong(timer -> timer.count()).sum();
        doThrow(new IllegalStateException("test database failure")).when(transactions)
                .saveProviderSuccess(anyLong(), anyLong(), any(), any(), any(), any(), anyLong(), any());
        send("운동 방법", 1L).andExpect(status().isServiceUnavailable());
        assertThat(chat.calls.get()).isEqualTo(1);
        assertThat(meterCount("app.ai.provider")).isEqualTo(providerCountBefore + 1);
        assertThat(registry.find("app.ai.provider").tag("outcome", "FAILURE")
                .timers().stream().mapToLong(timer -> timer.count()).sum()).isEqualTo(failuresBefore);
    }

    private long meterCount(String name) {
        return registry.find(name).timers().stream().mapToLong(timer -> timer.count()).sum();
    }

    private org.springframework.test.web.servlet.ResultActions send(String question, Long user)
            throws Exception {
        MockHttpServletRequestBuilder request =
                post("/api/ai/conversations/{id}/messages", conversation)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("message", question)));
        if (user != null) request.with(authenticatedUser(user));
        else {
            MvcResult csrfResult =
                    mvc.perform(get("/api/auth/csrf").with(authenticatedUser(1L))).andReturn();
            Cookie cookie =
                    java.util.Arrays.stream(csrfResult.getRequest().getCookies())
                            .filter(it -> it.getName().equals("XSRF-TOKEN"))
                            .findFirst()
                            .orElseThrow();
            assertThat(cookie).isNotNull();
            request.with(anonymous()).cookie(cookie).header("X-XSRF-TOKEN", cookie.getValue());
        }
        return mvc.perform(request);
    }

    private static AiPolicyGateway.AiPolicyAssessment assessment(
            String topic, double medical, double unsafe, double urgent) {
        Map<String, Double> probabilities =
                AiPolicyGateway.TOPICS.stream()
                        .collect(Collectors.toMap(it -> it, it -> it.equals(topic) ? 1.0 : 0.0));
        return new AiPolicyGateway.AiPolicyAssessment(
                "jev-1.13.0",
                medical,
                unsafe,
                urgent,
                0,
                new AiPolicyGateway.TopicAssessment(topic, probabilities, 0.95),
                100,
                10);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FakeConfiguration {
        @Bean
        @Primary
        FakePolicyGateway fakePolicy() {
            return new FakePolicyGateway();
        }

        @Bean
        @Primary
        FakeChatGateway fakeChat() {
            return new FakeChatGateway();
        }
    }

    static class FakePolicyGateway implements AiPolicyGateway {
        final AtomicInteger calls = new AtomicInteger();
        AiPolicyAssessment assessment;
        AiPolicyRequest last;
        boolean error;
        String errorCode = "TIMEOUT";
        volatile java.util.concurrent.CountDownLatch entered;
        volatile java.util.concurrent.CountDownLatch release;

        @Override
        public AiPolicyAssessment assess(AiPolicyRequest request) {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            calls.incrementAndGet();
            last = request;
            if (entered != null) {
                entered.countDown();
                try {
                    if (!release.await(5, java.util.concurrent.TimeUnit.SECONDS))
                        throw new AiPolicyUnavailableException("TIMEOUT");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AiPolicyUnavailableException("INTERRUPTED");
                }
            }
            if (error) throw new AiPolicyUnavailableException(errorCode);
            return assessment;
        }
    }

    static class FakeChatGateway implements AiChatGateway {
        final AtomicInteger calls = new AtomicInteger();
        AiModelRequest last;
        boolean fail;

        @Override
        public AiModelResponse chat(AiModelRequest request) {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            calls.incrementAndGet();
            last = request;
            if (fail) throw new IllegalStateException("test failure");
            return new AiModelResponse("참고 답변", "fake", "fake-model", 20, 10, 30);
        }

        @Override
        public String provider() {
            return "fake";
        }

        @Override
        public String model() {
            return "fake-model";
        }
    }
}
