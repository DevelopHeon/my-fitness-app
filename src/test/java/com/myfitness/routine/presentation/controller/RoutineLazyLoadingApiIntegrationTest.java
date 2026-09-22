package com.myfitness.routine.presentation.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
class RoutineLazyLoadingApiIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("트랜잭션이 종료된 뒤에도 루틴 목록과 상세 응답의 운동 목록을 조회할 수 있다")
    void returnsRoutineExercisesAfterApplicationTransactionEnds() throws Exception {
        long exerciseId = findDefaultExerciseId("벤치프레스");

        MvcResult created = mockMvc.perform(post("/api/routines")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Lazy Boundary Routine",
                                  "exercises":[
                                    {"exerciseType":"DEFAULT","exerciseId":%d}
                                  ]
                                }
                                """.formatted(exerciseId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exercises[0].exerciseName")
                        .value("벤치프레스"))
                .andReturn();

        long routineId = json(created).path("id").asLong();

        mockMvc.perform(get("/api/routines")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)].exercises[0].exerciseName"
                        .formatted(routineId))
                        .value("벤치프레스"));

        mockMvc.perform(get("/api/routines/{routineId}", routineId)
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].exerciseName")
                        .value("벤치프레스"));

        mockMvc.perform(delete("/api/routines/{routineId}", routineId)
                        .header("X-User-Id", 1L))
                .andExpect(status().isNoContent());
    }

    private long findDefaultExerciseId(String name) throws Exception {
        JsonNode exercises = json(mockMvc.perform(get("/api/exercises")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andReturn());

        for (JsonNode exercise : exercises) {
            if ("DEFAULT".equals(exercise.path("type").asText())
                    && name.equals(exercise.path("name").asText())) {
                return exercise.path("id").asLong();
            }
        }
        throw new AssertionError("기본 운동을 찾지 못했습니다: " + name);
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(
                result.getResponse().getContentAsString());
    }
}
