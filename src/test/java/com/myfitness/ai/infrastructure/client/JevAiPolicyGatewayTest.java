package com.myfitness.ai.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myfitness.ai.application.config.AiPolicyProperties;
import com.myfitness.ai.application.exception.AiPolicyUnavailableException;
import com.myfitness.ai.application.exception.AiPolicyUnavailableException.Code;
import com.myfitness.ai.application.port.out.AiPolicyGateway.AiPolicyAssessment;
import com.myfitness.ai.application.port.out.AiPolicyGateway.AiPolicyRequest;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

class JevAiPolicyGatewayTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final AiPolicyProperties properties = new AiPolicyProperties();
    private final AtomicInteger calls = new AtomicInteger();
    private HttpServer server;
    private ExecutorService executor;
    private volatile String body = response();
    private volatile int status = 200;
    private volatile long delayMs;
    private volatile JsonNode sent;
    private volatile String method;
    private volatile boolean disconnect;
    private volatile boolean authenticated;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.createContext(
                "/v1/systemone",
                exchange -> {
                    calls.incrementAndGet();
                    method = exchange.getRequestMethod();
                    authenticated =
                            "Bearer test-only-key"
                                    .equals(exchange.getRequestHeaders().getFirst("Authorization"));
                    sent = mapper.readTree(exchange.getRequestBody().readAllBytes());
                    if (disconnect) {
                        exchange.close();
                        return;
                    }
                    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(status, bytes.length);
                    try {
                        Thread.sleep(delayMs);
                        exchange.getResponseBody().write(bytes);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        exchange.close();
                    }
                });
        server.start();
        properties.setApiKey("test-only-key");
        properties.setEndpoint(
                URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/systemone"));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        executor.shutdownNow();
    }

    @Test
    @DisplayName("Jev 공식 HTTP 계약으로 5개 질문을 보내고 구조화된 평가만 반환한다")
    void sendsTypedQuestionsAndMapsAssessment() {
        try (JevAiPolicyGateway gateway = new JevAiPolicyGateway(properties, mapper)) {
            AiPolicyAssessment result =
                    gateway.assess(new AiPolicyRequest("오늘 운동 방법 알려줘", "WORKOUT", List.of()));
            assertThat(result.model()).isEqualTo("jev-1.13.0");
            assertThat(result.medicalDecision()).isEqualTo(0.02);
            assertThat(result.topic().choice()).isEqualTo("WORKOUT");
            assertThat(result.inputTokens()).isEqualTo(100);
            assertThat(method).isEqualTo("POST");
            assertThat(authenticated).isTrue();
            assertThat(sent.path("questions").size()).isEqualTo(5);
            assertThat(sent.path("questions").path("medical_decision").path("type").asText())
                    .isEqualTo("noul");
            assertThat(sent.path("questions").path("topic").path("criteria").size()).isEqualTo(7);
            assertThat(sent.path("state").path("current_question").asText())
                    .isEqualTo("오늘 운동 방법 알려줘");
            assertThat(sent.path("state").size()).isEqualTo(3);
            assertThat(sent.toString())
                    .doesNotContain("userId", "resourceId", "conversationId", "goldAction");
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 422, 429, 529, 500})
    @DisplayName("HTTP 공급자 실패를 허용으로 바꾸거나 자동 재시도하지 않는다")
    void rejectsHttpFailuresWithoutRetry(int errorStatus) {
        status = errorStatus;
        try (JevAiPolicyGateway gateway = new JevAiPolicyGateway(properties, mapper)) {
            assertThatThrownBy(() -> gateway.assess(request()))
                    .isInstanceOf(AiPolicyUnavailableException.class)
                    .satisfies(
                            error -> {
                                AiPolicyUnavailableException failure = (AiPolicyUnavailableException) error;
                                assertThat(failure.getCode()).isEqualTo(Code.HTTP_ERROR);
                                assertThat(failure.getHttpStatus()).isEqualTo(errorStatus);
                                assertThat(failure.getLogCode()).isEqualTo("HTTP_" + errorStatus);
                            });
            assertThat(calls.get()).isEqualTo(1);
        }
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "",
                "{}",
                "NaN",
                "missing",
                "wrong-type",
                "bad-sum",
                "bad-model",
                "bad-usage",
                "bad-choice"
            })
    @DisplayName("빈 응답과 누락·확률·모델·사용량 오류를 정책 장애로 처리한다")
    void rejectsInvalidResponses(String mutation) {
        body =
                switch (mutation) {
                    case "missing" -> response().replace("\"medical_decision\"", "\"unknown\"");
                    case "wrong-type" ->
                            response().replace("\"type\":\"noul\"", "\"type\":\"choice\"");
                    case "bad-choice" ->
                            response()
                                    .replace("\"choice\":\"WORKOUT\"", "\"choice\":\"NUTRITION\"");
                    case "bad-sum" -> response().replace("\"WORKOUT\":1", "\"WORKOUT\":0.5");
                    case "bad-model" -> response().replace("jev-1.13.0", "jev-other");
                    case "bad-usage" ->
                            response().replace("\"input_tokens\":100", "\"input_tokens\":-1");
                    default -> mutation;
                };
        try (JevAiPolicyGateway gateway = new JevAiPolicyGateway(properties, mapper)) {
            assertThatThrownBy(() -> gateway.assess(request()))
                    .isInstanceOf(AiPolicyUnavailableException.class);
        }
    }

    @ParameterizedTest
    @CsvSource({
        "topic,INVALID_RESPONSE_TOPIC",
        "usage,INVALID_RESPONSE_USAGE",
        "medical,INVALID_RESPONSE_MEDICAL_DECISION",
        "json,INVALID_RESPONSE_JSON"
    })
    @DisplayName("잘못된 응답은 원문 없이 실패한 검증 단계만 기록한다")
    void identifiesInvalidResponseStage(String mutation, String expectedCode) {
        body =
                switch (mutation) {
                    case "topic" ->
                            response().replace("\"choice\":\"WORKOUT\"", "\"choice\":\"NUTRITION\"");
                    case "usage" ->
                            response().replace("\"input_tokens\":100", "\"input_tokens\":-1");
                    case "medical" ->
                            response().replace("\"medical_decision\":{\"type\":\"noul\"",
                                    "\"medical_decision\":{\"type\":\"choice\"");
                    case "json" -> "{";
                    default -> throw new IllegalArgumentException("지원하지 않는 테스트 응답 변형");
                };
        try (JevAiPolicyGateway gateway = new JevAiPolicyGateway(properties, mapper)) {
            assertThatThrownBy(() -> gateway.assess(request()))
                    .isInstanceOf(AiPolicyUnavailableException.class)
                    .satisfies(
                            error ->
                                    assertThat(((AiPolicyUnavailableException) error).getCode())
                                            .isEqualTo(Code.valueOf(expectedCode)));
            assertThat(calls.get()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("응답 전 연결 끊김은 NETWORK 장애이며 요청을 재시도하지 않는다")
    void rejectsDisconnectedProviderWithoutRetry() {
        disconnect = true;
        try (JevAiPolicyGateway gateway = new JevAiPolicyGateway(properties, mapper)) {
            assertThatThrownBy(() -> gateway.assess(request()))
                    .isInstanceOf(AiPolicyUnavailableException.class)
                    .satisfies(
                            error ->
                                    assertThat(((AiPolicyUnavailableException) error).getCode())
                                            .isEqualTo(Code.NETWORK));
            assertThat(calls.get()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("응답 헤더 이후 본문이 지연되어도 전체 deadline에서 실패한다")
    void timesOutSlowResponseBody() {
        delayMs = 1000;
        properties.setRequestTimeout(Duration.ofMillis(100));
        long start = System.nanoTime();
        try (JevAiPolicyGateway gateway = new JevAiPolicyGateway(properties, mapper)) {
            assertThatThrownBy(() -> gateway.assess(request()))
                    .isInstanceOf(AiPolicyUnavailableException.class)
                    .satisfies(
                            error ->
                                    assertThat(((AiPolicyUnavailableException) error).getCode())
                                            .isEqualTo(Code.TIMEOUT));
            assertThat(Duration.ofNanos(System.nanoTime() - start).toMillis()).isLessThan(800);
        }
    }

    @Test
    @DisplayName("정책 키가 없으면 외부 호출 없이 설정 오류를 반환한다")
    void failsWithoutApiKey() {
        properties.setApiKey("");
        try (JevAiPolicyGateway gateway = new JevAiPolicyGateway(properties, mapper)) {
            assertThatThrownBy(() -> gateway.assess(request()))
                    .isInstanceOf(AiPolicyUnavailableException.class);
            assertThat(calls.get()).isZero();
        }
    }

    private static AiPolicyRequest request() {
        return new AiPolicyRequest("운동 방법", null, List.of());
    }

    private static String response() {
        return """
{"model":"jev-1.13.0","answers":{
 "medical_decision":{"type":"noul","noul":0.02},
 "unsafe_action":{"type":"noul","noul":0.01},
 "urgent_signal":{"type":"noul","noul":0.01},
 "policy_bypass":{"type":"noul","noul":0.01},
 "topic":{"type":"choice","choice":"WORKOUT","confidence":0.95,
  "probabilities":{"WORKOUT":1,"NUTRITION":0,"BODY":0,"GENERAL_FITNESS":0,"COMPOSITE":0,"OUT_OF_SCOPE":0,"AMBIGUOUS":0}}},
 "usage":{"input_tokens":100,"output_tokens":20}}
""";
    }
}
