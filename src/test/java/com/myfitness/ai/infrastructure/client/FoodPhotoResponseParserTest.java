package com.myfitness.ai.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class FoodPhotoResponseParserTest {
    @Test
    void rejectsContradictoryMalformedAndOutOfRangeResults() {
        FoodPhotoResponseParser parser = new FoodPhotoResponseParser(new ObjectMapper());
        String item = "{\"foodName\":\"밥\",\"servingDescription\":\"1그릇\",\"caloriesPerServing\":300}";
        for (String json : new String[] {
                "not json", "{}", "{\"status\":\"FOOD\",\"items\":[]}",
                "{\"status\":\"NOT_FOOD\",\"items\":[" + item + "]}",
                "{\"status\":\"UNKNOWN\",\"items\":[]}",
                "{\"status\":\"FOOD\",\"items\":[{}]}",
                "{\"status\":\"FOOD\",\"items\":[" + item.replace("300", "-1") + "]}",
                "{\"status\":\"FOOD\",\"items\":[" + item.replace("300", "1000000") + "]}",
                "{\"status\":\"FOOD\",\"items\":[" + item.replace("300", "300.001") + "]}",
                "{\"status\":\"FOOD\",\"items\":[" + item.replace("밥", " ") + "]}",
                "{\"status\":\"FOOD\",\"items\":[" + String.join(",", Collections.nCopies(6, item)) + "]}"}) {
            assertThatThrownBy(() -> parser.parse(json)).as(json).isInstanceOf(RuntimeException.class);
        }
    }

}
