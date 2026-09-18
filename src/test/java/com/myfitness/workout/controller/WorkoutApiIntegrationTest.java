package com.myfitness.workout.controller;

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
    @DisplayName("Workout 생성부터 세트 기록, 완료, 이전 기록 조회까지 수행한다")
    void completesWorkoutFlowAndReadsPreviousRecord() throws Exception {
        long exerciseId = createExercise();
        MvcResult workoutCreated = mockMvc.perform(post("/api/workouts")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workoutDate\":\"2026-09-18\",\"memo\":\"가슴 운동\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andReturn();
        long workoutId = json(workoutCreated).path("id").asLong();

        MvcResult exerciseAdded = mockMvc.perform(post("/api/workouts/{workoutId}/exercises", workoutId)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseId\":%d}".formatted(exerciseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].exerciseName").value("벤치프레스"))
                .andReturn();
        long workoutExerciseId = json(exerciseAdded).path("exercises").get(0).path("id").asLong();

        MvcResult setAdded = mockMvc.perform(post(
                        "/api/workouts/{workoutId}/exercises/{workoutExerciseId}/sets",
                        workoutId, workoutExerciseId)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weightKg\":60,\"reps\":10,\"completed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].sets[0].setNumber").value(1))
                .andReturn();
        long setId = json(setAdded).path("exercises").get(0).path("sets").get(0).path("id").asLong();

        mockMvc.perform(patch(
                        "/api/workouts/{workoutId}/exercises/{workoutExerciseId}/sets/{setId}",
                        workoutId, workoutExerciseId, setId)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weightKg\":65,\"reps\":8,\"completed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].sets[0].weightKg").value(65));

        mockMvc.perform(patch("/api/workouts/{workoutId}/complete", workoutId)
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(post("/api/workouts/{workoutId}/exercises/{workoutExerciseId}/sets",
                        workoutId, workoutExerciseId)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weightKg\":70,\"reps\":5,\"completed\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORKOUT_RULE_VIOLATION"));

        mockMvc.perform(get("/api/exercises/{exerciseId}/previous-record", exerciseId)
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workoutId").value(workoutId))
                .andExpect(jsonPath("$.sets[0].weightKg").value(65));

        mockMvc.perform(get("/api/workouts")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(workoutId))
                .andExpect(jsonPath("$[0].exercises[0].sets[0].weightKg").value(65));

        mockMvc.perform(get("/api/workouts/{workoutId}", workoutId)
                        .header("X-User-Id", 2L))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Workout에서 세트와 운동 종목을 삭제할 수 있다")
    void deletesSetAndExerciseFromWorkout() throws Exception {
        long exerciseId = createExercise();

        MvcResult workoutCreated = mockMvc.perform(post("/api/workouts")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn();
        long workoutId = json(workoutCreated).path("id").asLong();

        MvcResult exerciseAdded = mockMvc.perform(post("/api/workouts/{workoutId}/exercises", workoutId)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseId\":%d}".formatted(exerciseId)))
                .andExpect(status().isOk())
                .andReturn();
        long workoutExerciseId = json(exerciseAdded).path("exercises").get(0).path("id").asLong();

        MvcResult setAdded = mockMvc.perform(post(
                        "/api/workouts/{workoutId}/exercises/{workoutExerciseId}/sets",
                        workoutId, workoutExerciseId)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weightKg\":0,\"reps\":0,\"durationSeconds\":60,\"completed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].sets[0].durationSeconds").value(60))
                .andReturn();
        long setId = json(setAdded).path("exercises").get(0).path("sets").get(0).path("id").asLong();

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

    private long createExercise() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/exercises")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"벤치프레스\",\"category\":\"CHEST\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).path("id").asLong();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
