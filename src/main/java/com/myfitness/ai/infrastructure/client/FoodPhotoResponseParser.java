package com.myfitness.ai.infrastructure.client;

import com.myfitness.ai.application.exception.AiProviderUnavailableException;
import com.myfitness.ai.application.exception.AiProviderUnavailableException.Code;
import com.myfitness.ai.application.port.out.FoodPhotoGateway.PhotoResponse;
import com.myfitness.ai.domain.model.FoodPhotoAnalysis;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class FoodPhotoResponseParser {
    private final ObjectMapper mapper;

    public FoodPhotoResponseParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public PhotoResponse read(ChatResponse response, String model) {
        Generation generation = requireCompleteGeneration(response);
        FoodPhotoAnalysis analysis;
        try {
            analysis = parse(generation.getOutput().getText());
        } catch (RuntimeException exception) {
            throw unavailable(Code.INVALID_RESPONSE);
        }
        Usage usage = response.getMetadata() == null ? null : response.getMetadata().getUsage();
        if (usage instanceof EmptyUsage) {
            usage = null;
        }
        return new PhotoResponse(
                analysis,
                model,
                usage == null ? null : usage.getPromptTokens(),
                usage == null ? null : usage.getCompletionTokens(),
                usage == null ? null : usage.getTotalTokens());
    }

    private Generation requireCompleteGeneration(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw unavailable(Code.INVALID_RESPONSE);
        }
        Generation generation = response.getResult();
        Object refusal = generation.getOutput().getMetadata().get("refusal");
        if (refusal != null && !refusal.toString().isBlank()) {
            throw unavailable(Code.MODEL_REFUSAL);
        }
        String finishReason = generation.getMetadata() == null ? null
                : generation.getMetadata().getFinishReason();
        if (finishReason != null && !finishReason.isBlank() && !"stop".equalsIgnoreCase(finishReason)) {
            throw unavailable(Code.INCOMPLETE_RESPONSE);
        }
        return generation;
    }

    FoodPhotoAnalysis parse(String text) {
        JsonNode json = mapper.readTree(text);
        if (json == null || !json.isObject() || json.size() != 2 || !json.path("status").isTextual()
                || !json.path("items").isArray()) {
            throw new IllegalArgumentException("INVALID_RESPONSE_STRUCTURE");
        }
        List<FoodPhotoAnalysis.Item> items = new ArrayList<>();
        for (JsonNode item : json.path("items")) {
            if (!item.isObject() || item.size() != 3 || !item.path("foodName").isTextual()
                    || !item.path("servingDescription").isTextual() || !item.path("caloriesPerServing").isNumber()) {
                throw new IllegalArgumentException("INVALID_RESPONSE_ITEM");
            }
            items.add(new FoodPhotoAnalysis.Item(item.path("foodName").asText(),
                    item.path("servingDescription").asText(), item.path("caloriesPerServing").decimalValue()));
        }
        return new FoodPhotoAnalysis(FoodPhotoAnalysis.Status.valueOf(json.path("status").asText()), items);
    }

    private AiProviderUnavailableException unavailable(Code code) {
        return new AiProviderUnavailableException(
                "음식 사진 분석을 완료하지 못했습니다. 잠시 후 다시 시도하거나 직접 입력해주세요.", null, code);
    }
}
