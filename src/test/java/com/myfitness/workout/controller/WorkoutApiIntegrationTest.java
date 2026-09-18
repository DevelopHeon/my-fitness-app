package com.myfitness.workout.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@Transactional
class WorkoutApiIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("기본 운동 카탈로그와 사용자별 커스텀 운동을 분리해 제공한다")
    void providesDefaultCatalogAndUserCustomExercisesSeparately() throws Exception {
        MvcResult defaults = mockMvc.perform(get("/api/exercises")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == '벤치프레스')].type").value("DEFAULT"))
                .andExpect(jsonPath("$[?(@.name == '스쿼트')].category").value("LEGS"))
                .andReturn();

        assertThat(json(defaults).size()).isGreaterThanOrEqualTo(40);

        mockMvc.perform(post("/api/exercises/custom")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"나만의 프레스","category":"CHEST"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("CUSTOM"))
                .andExpect(jsonPath("$.name").value("나만의 프레스"));

        mockMvc.perform(get("/api/exercises")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == '나만의 프레스')].type").value("CUSTOM"));

        mockMvc.perform(get("/api/exercises")
                        .header("X-User-Id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == '나만의 프레스')]").isEmpty());
    }

    @Test
    @DisplayName("Workout 생성부터 기본 운동 세트 기록, 완료, 이전 기록 조회까지 수행한다")
    void completesWorkoutFlowAndReadsPreviousRecord() throws Exception {
        long exerciseId = findDefaultExerciseId("벤치프레스");

        MvcResult workoutCreated = mockMvc.perform(post("/api/workouts")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workoutDate":"2026-09-18","memo":"가슴 운동"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andReturn();
        long workoutId = json(workoutCreated).path("id").asLong();

        MvcResult exerciseAdded = mockMvc.perform(post(
                        "/api/workouts/{workoutId}/exercises", workoutId)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exerciseType":"DEFAULT","exerciseId":%d}
                                """.formatted(exerciseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].exerciseName").value("벤치프레스"))
                .andExpect(jsonPath("$.exercises[0].exerciseType").value("DEFAULT"))
                .andReturn();
        long workoutExerciseId =
                json(exerciseAdded).path("exercises").get(0).path("id").asLong();

        MvcResult setAdded = mockMvc.perform(post(
                        "/api/workouts/{workoutId}/exercises/{workoutExerciseId}/sets",
                        workoutId, workoutExerciseId)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"weightKg":60,"reps":10,"completed":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].sets[0].setNumber").value(1))
                .andReturn();
        long setId = json(setAdded).path("exercises")
                .get(0).path("sets").get(0).path("id").asLong();

        mockMvc.perform(patch(
                        "/api/workouts/{workoutId}/exercises/{workoutExerciseId}/sets/{setId}",
                        workoutId, workoutExerciseId, setId)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"weightKg":65,"reps":8,"completed":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].sets[0].weightKg").value(65));

        mockMvc.perform(patch("/api/workouts/{workoutId}/complete", workoutId)
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get(
                        "/api/exercises/DEFAULT/{exerciseId}/previous-record",
                        exerciseId)
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workoutId").value(workoutId))
                .andExpect(jsonPath("$.exerciseType").value("DEFAULT"))
                .andExpect(jsonPath("$.sets[0].weightKg").value(65));

        mockMvc.perform(get("/api/workouts")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].exercises[0].exerciseName")
                        .value("벤치프레스"))
                .andExpect(jsonPath("$[0].exercises[0].sets[0].weightKg").value(65));

        mockMvc.perform(get("/api/workouts/{workoutId}", workoutId)
                        .header("X-User-Id", 2L))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("사용자 커스텀 운동의 세트와 운동 종목을 삭제할 수 있다")
    void deletesSetAndCustomExerciseFromWorkout() throws Exception {
        JsonNode custom = createCustomExercise("개인 인터벌", "ABS");
        long exerciseId = custom.path("id").asLong();

        MvcResult workoutCreated = mockMvc.perform(post("/api/workouts")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn();
        long workoutId = json(workoutCreated).path("id").asLong();

        MvcResult exerciseAdded = mockMvc.perform(post(
                        "/api/workouts/{workoutId}/exercises", workoutId)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exerciseType":"CUSTOM","exerciseId":%d}
                                """.formatted(exerciseId)))
                .andExpect(status().isOk())
                .andReturn();
        long workoutExerciseId =
                json(exerciseAdded).path("exercises").get(0).path("id").asLong();

        MvcResult setAdded = mockMvc.perform(post(
                        "/api/workouts/{workoutId}/exercises/{workoutExerciseId}/sets",
                        workoutId, workoutExerciseId)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"weightKg":0,"reps":0,"durationSeconds":60,"completed":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].sets[0].durationSeconds").value(60))
                .andReturn();
        long setId = json(setAdded).path("exercises")
                .get(0).path("sets").get(0).path("id").asLong();

        mockMvc.perform(delete(
                        "/api/workouts/{workoutId}/exercises/{workoutExerciseId}/sets/{setId}",
                        workoutId, workoutExerciseId, setId)
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].sets").isEmpty());

        mockMvc.perform(delete(
                        "/api/workouts/{workoutId}/exercises/{workoutExerciseId}",
                        workoutId, workoutExerciseId)
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises").isEmpty());
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

    private JsonNode createCustomExercise(String name, String category) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/exercises/custom")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","category":"%s"}
                                """.formatted(name, category)))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result);
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
