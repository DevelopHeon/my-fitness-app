package com.myfitness.routine.controller;

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
class RoutineApiIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("루틴을 생성·수정하고 저장된 순서로 Workout을 시작하며 직전 기록을 반환한다")
    void managesRoutineAndStartsWorkoutWithPreviousRecord() throws Exception {
        long benchPressId = createExercise("벤치프레스", "CHEST");
        long squatId = createExercise("스쿼트", "LEGS");
        createCompletedWorkoutWithRecord(benchPressId);

        MvcResult created = mockMvc.perform(post("/api/routines")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Push","exerciseIds":[%d,%d]}
                                """.formatted(benchPressId, squatId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Push"))
                .andExpect(jsonPath("$.exercises[0].exerciseName").value("벤치프레스"))
                .andExpect(jsonPath("$.exercises[1].exerciseName").value("스쿼트"))
                .andReturn();
        long routineId = json(created).path("id").asLong();

        mockMvc.perform(put("/api/routines/{routineId}", routineId)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Push & Legs","exerciseIds":[%d,%d]}
                                """.formatted(squatId, benchPressId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Push & Legs"))
                .andExpect(jsonPath("$.exercises[0].exerciseName").value("스쿼트"))
                .andExpect(jsonPath("$.exercises[0].orderIndex").value(1))
                .andExpect(jsonPath("$.exercises[1].exerciseName").value("벤치프레스"))
                .andExpect(jsonPath("$.exercises[1].orderIndex").value(2));

        mockMvc.perform(get("/api/routines")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(routineId));

        mockMvc.perform(post("/api/routines/{routineId}/workouts", routineId)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workout.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.workout.exercises[0].exerciseName").value("스쿼트"))
                .andExpect(jsonPath("$.workout.exercises[1].exerciseName").value("벤치프레스"))
                .andExpect(jsonPath("$.previousRecords[0].exerciseName").value("벤치프레스"))
                .andExpect(jsonPath("$.previousRecords[0].sets[0].weightKg").value(80));
    }

    @Test
    @DisplayName("루틴은 사용자별로 격리되고 소유자는 삭제할 수 있다")
    void protectsRoutineOwnershipAndAllowsDeletion() throws Exception {
        long exerciseId = createExercise("랫풀다운", "BACK");
        MvcResult created = mockMvc.perform(post("/api/routines")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Pull","exerciseIds":[%d]}
                                """.formatted(exerciseId)))
                .andExpect(status().isCreated())
                .andReturn();
        long routineId = json(created).path("id").asLong();

        mockMvc.perform(get("/api/routines/{routineId}", routineId)
                        .header("X-User-Id", 2L))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/routines/{routineId}", routineId)
                        .header("X-User-Id", 1L))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/routines/{routineId}", routineId)
                        .header("X-User-Id", 1L))
                .andExpect(status().isNotFound());
    }

    private long createExercise(String name, String category) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/exercises")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","category":"%s"}
                                """.formatted(name, category)))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).path("id").asLong();
    }

    private void createCompletedWorkoutWithRecord(long exerciseId) throws Exception {
        MvcResult workout = mockMvc.perform(post("/api/workouts")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"workoutDate\":\"2026-09-17\"" + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        long workoutId = json(workout).path("id").asLong();

        MvcResult added = mockMvc.perform(post("/api/workouts/{workoutId}/exercises", workoutId)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"exerciseId\":" + exerciseId + "}"))
                .andExpect(status().isOk())
                .andReturn();
        long workoutExerciseId = json(added).path("exercises").get(0).path("id").asLong();

        mockMvc.perform(post(
                        "/api/workouts/{workoutId}/exercises/{workoutExerciseId}/sets",
                        workoutId, workoutExerciseId)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"weightKg\":80,\"reps\":8,\"completed\":true" + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/workouts/{workoutId}/complete", workoutId)
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk());
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
