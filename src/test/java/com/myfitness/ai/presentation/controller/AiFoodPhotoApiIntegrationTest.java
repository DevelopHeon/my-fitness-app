package com.myfitness.ai.presentation.controller;

import static com.myfitness.test.security.TestSecurity.authenticatedUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.myfitness.ai.application.port.out.AiChatGateway;
import com.myfitness.ai.application.port.out.AiMessageRepositoryPort;
import com.myfitness.ai.application.port.out.AiPolicyGateway;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import com.myfitness.ai.application.service.FoodPhotoTransactionService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {"spring.ai.model.chat=openai", "spring.ai.openai.api-key=test"})
class AiFoodPhotoApiIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired MeterRegistry registry;
    private long policyCountBefore;
    private long providerCountBefore;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired AiMessageRepositoryPort messages;
    @MockitoBean ChatModel model;
    @MockitoBean AiChatGateway textGateway;
    @MockitoBean AiPolicyGateway policyGateway;
    @MockitoSpyBean FoodPhotoTransactionService transactions;
    private MockMvc mvc;
    private long conversationId;

    @BeforeEach
    void setUp() throws Exception {
        policyCountBefore = meterCount("app.ai.policy");
        providerCountBefore = meterCount("app.ai.provider");
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        jdbc.update("delete from ai_request_logs");
        jdbc.update("delete from ai_messages");
        jdbc.update("delete from ai_conversations");
        MvcResult created = mvc.perform(post("/api/ai/conversations").with(authenticatedUser(1L)))
                .andExpect(status().isCreated()).andReturn();
        conversationId = mapper.readTree(created.getResponse().getContentAsString()).path("id").asLong();
    }

    @Test
    void persistsFoodResultWithoutSavingMealOrCallingTextPolicy() throws Exception {
        int beforeMeals = jdbc.queryForObject("select count(*) from meal_foods", Integer.class);
        respond("""
                {"status":"FOOD","items":[{"foodName":"비빔밥","servingDescription":"일반적인 1그릇",
                "caloriesPerServing":500}]}
                """);
        send(1L).andExpect(status().isOk())
                .andExpect(jsonPath("$.assistantMessage.foodPhotoResult.items[0].foodName").value("비빔밥"));
        mvc.perform(get("/api/ai/conversations/{id}/messages", conversationId).with(authenticatedUser(1L)))
                .andExpect(jsonPath("$[1].foodPhotoResult.items[0].caloriesPerServing").value(500));
        assertThat(jdbc.queryForObject("select count(*) from ai_request_logs where request_kind='FOOD_PHOTO'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select input_tokens from ai_request_logs where request_kind='FOOD_PHOTO'", Integer.class))
                .isNull();
        assertThat(messages.findAcceptedByConversationId(conversationId)).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from meal_foods", Integer.class)).isEqualTo(beforeMeals);
        assertThat(meterCount("app.ai.provider")).isEqualTo(providerCountBefore + 1);
        assertThat(meterCount("app.ai.policy")).isEqualTo(policyCountBefore);
        verify(model, times(1)).call(any(Prompt.class));
        verifyNoInteractions(textGateway, policyGateway);
    }

    @Test
    void rejectsNonFoodAndUncertainWithoutCandidates() throws Exception {
        for (String status : new String[] {"NOT_FOOD", "UNCERTAIN"}) {
            respond("{\"status\":\"" + status + "\",\"items\":[]}");
            send(1L).andExpect(status().isOk())
                    .andExpect(jsonPath("$.assistantMessage.foodPhotoResult.status").value(status))
                    .andExpect(jsonPath("$.assistantMessage.foodPhotoResult.items").isEmpty());
        }
        assertThat(meterCount("app.ai.provider")).isEqualTo(providerCountBefore + 2);
        verify(model, times(2)).call(any(Prompt.class));
        verifyNoInteractions(textGateway, policyGateway);
    }

    @Test
    void failsClosedForInvalidOutputAndProviderFailure() throws Exception {
        respond("{\"status\":\"FOOD\",\"items\":[]}");
        send(1L).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_PROVIDER_UNAVAILABLE"));
        when(model.call(any(Prompt.class))).thenThrow(new RuntimeException("private provider response"));
        send(1L).andExpect(status().isServiceUnavailable());
        assertThat(jdbc.queryForObject("select count(*) from ai_messages where role='ASSISTANT'", Integer.class))
                .isZero();
        assertThat(jdbc.queryForList("select error_code from ai_request_logs order by id", String.class))
                .containsExactly("INVALID_RESPONSE", "TRANSPORT_ERROR");
        assertThat(meterCount("app.ai.provider")).isEqualTo(providerCountBefore + 2);
        verify(model, times(2)).call(any(Prompt.class));
        verifyNoInteractions(textGateway, policyGateway);
    }

    @Test
    void validatesOwnershipAndFileBeforeCallingModel() throws Exception {
        send(2L).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/ai/conversations/{id}/food-photos", conversationId)
                        .file(new MockMultipartFile("image", "photo.jpg", "image/jpeg", new byte[] {1, 2}))
                        .with(authenticatedUser(1L)))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select count(*) from ai_messages", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from ai_request_logs", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select title from ai_conversations where id=?", String.class, conversationId))
                .isEqualTo("새 대화");
        assertThat(meterCount("app.ai.provider")).isEqualTo(providerCountBefore);
        verifyNoInteractions(model, textGateway, policyGateway);
    }

    @Test
    void rejectsMultipleImagePartsBeforeProviderCall() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "jpg", output);
        byte[] image = output.toByteArray();
        respond("{\"status\":\"NOT_FOOD\",\"items\":[]}");
        mvc.perform(multipart("/api/ai/conversations/{id}/food-photos", conversationId)
                        .file(new MockMultipartFile("image", "a.jpg", "image/jpeg", image))
                        .file(new MockMultipartFile("image", "b.jpg", "image/jpeg", image))
                        .with(authenticatedUser(1L)))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select count(*) from ai_messages", Integer.class)).isZero();
        assertThat(meterCount("app.ai.provider")).isEqualTo(providerCountBefore);
        verifyNoInteractions(model, textGateway, policyGateway);
    }

    @Test
    void doesNotCountDatabaseSaveFailureAsAnotherPhotoCall() throws Exception {
        long failuresBefore = registry.find("app.ai.provider").tag("outcome", "FAILURE")
                .timers().stream().mapToLong(timer -> timer.count()).sum();
        respond("{\"status\":\"NOT_FOOD\",\"items\":[]}");
        doThrow(new IllegalStateException("test database failure")).when(transactions)
                .savePhotoSuccess(anyLong(), anyLong(), any(), any(), any(), anyLong());
        assertThatThrownBy(() -> send(1L)).hasRootCauseInstanceOf(IllegalStateException.class);
        assertThat(meterCount("app.ai.provider")).isEqualTo(providerCountBefore + 1);
        assertThat(registry.find("app.ai.provider").tag("outcome", "FAILURE")
                .timers().stream().mapToLong(timer -> timer.count()).sum()).isEqualTo(failuresBefore);
        verify(model, times(1)).call(any(Prompt.class));
    }

    private long meterCount(String name) {
        return registry.find(name).timers().stream().mapToLong(timer -> timer.count()).sum();
    }

    private void respond(String json) {
        when(model.call(any(Prompt.class))).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return new ChatResponse(java.util.List.of(new Generation(new AssistantMessage(json))));
        });
    }

    private ResultActions send(long userId) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "jpg", output);
        return mvc.perform(multipart("/api/ai/conversations/{id}/food-photos", conversationId)
                .file(new MockMultipartFile("image", "food.jpg", "image/jpeg", output.toByteArray()))
                .with(authenticatedUser(userId)));
    }
}
