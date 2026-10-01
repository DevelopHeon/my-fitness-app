package com.myfitness.ai.application.support;

import com.myfitness.ai.application.dto.response.AiMessageResult;
import com.myfitness.ai.domain.model.AiMessage;
import com.myfitness.ai.domain.model.FoodPhotoAnalysis;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class AiMessageResultMapper {
    private final ObjectMapper mapper;

    public AiMessageResultMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public AiMessageResult map(AiMessage message) {
        FoodPhotoAnalysis analysis = message.getFoodPhotoResult() == null
                ? null : mapper.readValue(message.getFoodPhotoResult(), FoodPhotoAnalysis.class);
        return AiMessageResult.from(message, analysis);
    }
}
