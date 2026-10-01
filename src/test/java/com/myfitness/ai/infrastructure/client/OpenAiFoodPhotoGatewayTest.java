package com.myfitness.ai.infrastructure.client;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.myfitness.ai.application.dto.request.FoodPhotoCommand;
import com.myfitness.ai.application.exception.AiProviderUnavailableException;
import com.myfitness.ai.application.exception.InvalidFoodPhotoException;
import com.myfitness.ai.application.port.out.FoodPhotoGateway.PhotoResponse;
import com.myfitness.ai.domain.model.FoodPhotoAnalysis;
import com.sun.net.httpserver.HttpServer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class OpenAiFoodPhotoGatewayTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void sendsImageAndStrictSchemaUsingRealSdkAndAcceptsEmptyRefusal() throws Exception {
        AtomicReference<JsonNode> request = new AtomicReference<>();
        AtomicInteger calls = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            calls.incrementAndGet();
            request.set(mapper.readTree(exchange.getRequestBody().readAllBytes()));
            String content = """
                    {"status":"FOOD","items":[{"foodName":"밥","servingDescription":"일반적인 1그릇","caloriesPerServing":300}]}
                    """;
            byte[] body = mapper.writeValueAsBytes(Map.of("id", "local-test", "object", "chat.completion",
                    "created", 1, "model", "gpt-4o-mini", "choices", List.of(Map.of("index", 0,
                    "finish_reason", "stop", "message", Map.of("role", "assistant", "content", content))),
                    "usage", Map.of("prompt_tokens", 20, "completion_tokens", 30, "total_tokens", 50)));
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            ChatModel model = OpenAiChatModel.builder().options(OpenAiChatOptions.builder()
                    .apiKey("test").baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                    .maxRetries(0).build()).build();
            OpenAiFoodPhotoGateway gateway = gateway(model);
            PhotoResponse result = gateway.analyze(gateway.prepare(photo()));
            assertThat(result.analysis().status()).isEqualTo(FoodPhotoAnalysis.Status.FOOD);
            assertThat(result.inputTokens()).isEqualTo(20);
            assertThat(calls).hasValue(1);
            assertThat(request.get().path("response_format").path("type").asText()).isEqualTo("json_schema");
            assertThat(request.get().path("response_format").path("json_schema").path("strict").asBoolean()).isTrue();
            assertThat(request.get().path("store").asBoolean()).isFalse();
            assertThat(request.get().path("messages").get(1).path("content").get(1).path("image_url")
                    .path("url").asText()).startsWith("data:image/jpeg;base64,");
        } finally { server.stop(0); }
    }

    @Test
    void stopsOnRefusalIncompleteAndTimeoutWithoutRetry() throws Exception {
        ChatModel model = mock(ChatModel.class);
        OpenAiFoodPhotoGateway gateway = gateway(model);
        byte[] input = gateway.prepare(photo());
        when(model.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(
                AssistantMessage.builder().content("{\"status\":\"NOT_FOOD\",\"items\":[]}")
                        .properties(Map.of("refusal", "refused")).build()))));
        assertThatThrownBy(() -> gateway.analyze(input)).isInstanceOf(AiProviderUnavailableException.class);
        when(model.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(
                new AssistantMessage("{\"status\":\"NOT_FOOD\",\"items\":[]}"),
                ChatGenerationMetadata.builder().finishReason("length").build()))));
        assertThatThrownBy(() -> gateway.analyze(input)).isInstanceOf(AiProviderUnavailableException.class);
        when(model.call(any(Prompt.class))).thenThrow(new ResourceAccessException(
                "private", new SocketTimeoutException()));
        assertThatThrownBy(() -> gateway.analyze(input)).isInstanceOf(AiProviderUnavailableException.class)
                .hasMessageNotContaining("private")
                .satisfies(error -> assertThat(((AiProviderUnavailableException) error).getErrorCode()).isEqualTo("TIMEOUT"));
        verify(model, times(3)).call(any(Prompt.class));
    }

    @Test
    void rejectsOversizedUnsupportedAndPixelBombBeforeRemoteCall() throws Exception {
        ChatModel model = mock(ChatModel.class);
        OpenAiFoodPhotoGateway gateway = gateway(model);
        assertThatThrownBy(() -> gateway.prepare(new FoodPhotoCommand(new byte[5 * 1024 * 1024 + 1], "image/jpeg")))
                .isInstanceOf(InvalidFoodPhotoException.class);
        assertThatThrownBy(() -> gateway.prepare(new FoodPhotoCommand(photo().bytes(), "image/webp")))
                .isInstanceOf(InvalidFoodPhotoException.class);
        byte[] jpeg = photo().bytes();
        for (int i = 0; i < jpeg.length - 8; i++) {
            if ((jpeg[i] & 255) == 255 && (jpeg[i + 1] & 255) == 192) {
                // SOF metadata is modified without allocating or decoding a huge raster.
                jpeg[i + 5] = 0x27;
                jpeg[i + 6] = 0x10;
                jpeg[i + 7] = 0x27;
                jpeg[i + 8] = 0x10;
                break;
            }
        }
        assertThatThrownBy(() -> gateway.prepare(new FoodPhotoCommand(jpeg, "image/jpeg")))
                .isInstanceOf(InvalidFoodPhotoException.class);
        verifyNoInteractions(model);
    }

    private OpenAiFoodPhotoGateway gateway(ChatModel model) {
        @SuppressWarnings("unchecked")
        ObjectProvider<ChatModel> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(model);
        return new OpenAiFoodPhotoGateway(provider, new MockEnvironment()
                .withProperty("spring.ai.model.chat", "openai")
                .withProperty("spring.ai.openai.api-key", "test"),
                new FoodPhotoImagePreparer(), new FoodPhotoResponseParser(mapper));
    }

    private FoodPhotoCommand photo() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "jpeg", out);
        return new FoodPhotoCommand(out.toByteArray(), "image/jpeg");
    }
}
