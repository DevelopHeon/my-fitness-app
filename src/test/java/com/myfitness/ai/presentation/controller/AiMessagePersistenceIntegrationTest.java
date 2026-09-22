package com.myfitness.ai.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myfitness.ai.application.port.out.AiChatGateway;
import com.myfitness.ai.application.port.out.AiChatGateway.AiModelRequest;
import com.myfitness.ai.application.port.out.AiChatGateway.AiModelResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@Import(AiMessagePersistenceIntegrationTest.BlockingAiConfiguration.class)
class AiMessagePersistenceIntegrationTest {
    private static final long USER_ID = 8301L;

    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired BlockingAiChatGateway blockingGateway;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        blockingGateway.reset();
    }

    @Test
    @DisplayName("Provider 응답 대기 중에도 User Message를 먼저 커밋하고 완료 후 Assistant Message를 저장한다")
    void persistsUserMessageBeforeProviderResponseCompletes() throws Exception {
        long conversationId = createConversation();

        CompletableFuture<MvcResult> responseFuture =
                CompletableFuture.supplyAsync(() -> send(conversationId));

        assertThat(blockingGateway.awaitRequest()).isTrue();

        assertThat(messageCount(conversationId, "USER"))
                .isEqualTo(1);
        assertThat(messageCount(conversationId, "ASSISTANT"))
                .isZero();

        blockingGateway.release();
        MvcResult result = responseFuture.get(5, TimeUnit.SECONDS);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(messageCount(conversationId, "USER"))
                .isEqualTo(1);
        assertThat(messageCount(conversationId, "ASSISTANT"))
                .isEqualTo(1);
    }

    private long createConversation() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/ai/conversations")
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(
                        result.getResponse().getContentAsString())
                .path("id")
                .asLong();
    }

    private MvcResult send(long conversationId) {
        try {
            return mockMvc.perform(post(
                            "/api/ai/conversations/{id}/messages",
                            conversationId)
                            .header("X-User-Id", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"message":"운동 회복 방법 알려줘"}
                                    """))
                    .andExpect(status().isOk())
                    .andReturn();
        } catch (Exception exception) {
            throw new CompletionException(exception);
        }
    }

    private int messageCount(long conversationId, String role) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from ai_messages
                where conversation_id = ?
                  and role = ?
                """,
                Integer.class,
                conversationId,
                role);
        return count == null ? 0 : count;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class BlockingAiConfiguration {
        @Bean
        @Primary
        BlockingAiChatGateway blockingAiChatGateway() {
            return new BlockingAiChatGateway();
        }
    }

    static class BlockingAiChatGateway implements AiChatGateway {
        private volatile CountDownLatch requested = new CountDownLatch(1);
        private volatile CountDownLatch released = new CountDownLatch(1);

        @Override
        public AiModelResponse chat(AiModelRequest request) {
            requested.countDown();
            try {
                if (!released.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "fake provider release timeout");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "fake provider interrupted",
                        exception);
            }

            return new AiModelResponse(
                    "fake saved answer",
                    "fake",
                    "fake-model",
                    30,
                    10,
                    40);
        }

        @Override
        public String provider() {
            return "fake";
        }

        @Override
        public String model() {
            return "fake-model";
        }

        boolean awaitRequest() throws InterruptedException {
            return requested.await(5, TimeUnit.SECONDS);
        }

        void release() {
            released.countDown();
        }

        void reset() {
            requested = new CountDownLatch(1);
            released = new CountDownLatch(1);
        }
    }
}
