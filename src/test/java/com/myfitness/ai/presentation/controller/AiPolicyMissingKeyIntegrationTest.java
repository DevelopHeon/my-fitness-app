package com.myfitness.ai.presentation.controller;

import static com.myfitness.test.security.TestSecurity.authenticatedUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myfitness.ai.application.context.AiContextBuilder;
import com.myfitness.ai.application.port.out.AiChatGateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "app.ai.policy.api-key=")
class AiPolicyMissingKeyIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean AiChatGateway chat;
    @MockitoSpyBean AiContextBuilder contextBuilder;

    @Test
    @DisplayName("실제 JEV Adapter의 key 누락은 운동 질문에도 503이며 keyword 우회와 답변 생성이 없다")
    void failsClosedWithRealAdapterWithoutKey() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        long conversation =
                mapper.readTree(
                                mvc.perform(
                                                post("/api/ai/conversations")
                                                        .with(authenticatedUser(9011L)))
                                        .andExpect(status().isCreated())
                                        .andReturn()
                                        .getResponse()
                                        .getContentAsString())
                        .path("id")
                        .asLong();
        mvc.perform(
                        post("/api/ai/conversations/{id}/messages", conversation)
                                .with(authenticatedUser(9011L))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"message\":\"운동 방법을 알려줘\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AI_POLICY_UNAVAILABLE"));
        verifyNoInteractions(chat, contextBuilder);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from ai_messages where conversation_id=?",
                                Integer.class,
                                conversation))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "select policy_error_code from ai_request_logs where"
                                    + " conversation_id=?",
                                String.class,
                                conversation))
                .isEqualTo("CONFIGURATION");
    }
}
