package com.myfitness.ai.infrastructure.springai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.myfitness.ai.application.port.out.AiChatGateway.AiModelRequest;
import com.myfitness.ai.application.port.out.AiChatGateway.AiModelResponse;
import com.myfitness.ai.application.port.out.AiChatGateway.HistoryMessage;
import com.myfitness.ai.domain.model.AiMessageRole;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;

import java.util.List;

class SpringAiChatGatewayTest {

    @Test
    @DisplayName("Spring AI Gateway는 Context와 History를 ChatModel에 전달하고 token usage를 반환한다")
    void sendsPromptAndReturnsUsageMetadata() {
        ChatModel chatModel = mock(ChatModel.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<ChatModel> provider = mock(ObjectProvider.class);
        Environment environment = mock(Environment.class);
        ChatResponse response = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        ChatResponseMetadata metadata = mock(ChatResponseMetadata.class);
        Usage usage = mock(Usage.class);

        when(provider.getIfAvailable()).thenReturn(chatModel);
        when(environment.getProperty("spring.ai.model.chat", "none")).thenReturn("openai");
        when(environment.getProperty("spring.ai.openai.chat.model", "gpt-4o-mini"))
                .thenReturn("gpt-4o-mini");
        when(chatModel.call(any(Prompt.class))).thenReturn(response);
        when(response.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(new AssistantMessage("개인화 답변"));
        when(response.getMetadata()).thenReturn(metadata);
        when(metadata.getUsage()).thenReturn(usage);
        when(usage.getPromptTokens()).thenReturn(120);
        when(usage.getCompletionTokens()).thenReturn(40);
        when(usage.getTotalTokens()).thenReturn(160);

        SpringAiChatGateway gateway = new SpringAiChatGateway(provider, environment);

        AiModelResponse result =
                gateway.chat(
                        new AiModelRequest(
                                "system policy",
                                "[운동 기록] 최근 7일 3회",
                                List.of(
                                        new HistoryMessage(AiMessageRole.USER, "지난 질문"),
                                        new HistoryMessage(AiMessageRole.ASSISTANT, "지난 답변")),
                                "이번 질문"));

        assertThat(result.content()).isEqualTo("개인화 답변");
        assertThat(result.provider()).isEqualTo("openai");
        assertThat(result.model()).isEqualTo("gpt-4o-mini");
        assertThat(result.inputTokens()).isEqualTo(120);
        assertThat(result.outputTokens()).isEqualTo(40);
        assertThat(result.totalTokens()).isEqualTo(160);

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());

        List<Message> messages = promptCaptor.getValue().getInstructions();
        assertThat(messages).hasSize(5);
        assertThat(messages.get(0).getText()).contains("system policy");
        assertThat(messages.get(1).getText()).contains("최근 7일 3회");
        assertThat(messages.get(2).getText()).isEqualTo("지난 질문");
        assertThat(messages.get(3).getText()).isEqualTo("지난 답변");
        assertThat(messages.get(4).getText()).isEqualTo("이번 질문");
    }
}
