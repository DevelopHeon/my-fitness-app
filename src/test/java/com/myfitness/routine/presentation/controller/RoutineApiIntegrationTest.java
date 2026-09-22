package com.myfitness.routine.presentation.controller;

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
    @DisplayName("기본 운동과 커스텀 운동을 섞은 루틴으로 Workout을 시작하고 직전 기록을 반환한다")
    void managesMixedRoutineAndStartsWorkoutWithPreviousRecord() throws Exception {
        long benchPressId = findDefaultExerciseId("벤치프레스");
        long squatId = findDefaultExerciseId("스쿼트");
        JsonNode custom = createCustomExercise("나만의 레그 프레스", "LEGS");
        long customId = custom.path("id").asLong();

        createCompletedWorkoutWithRecord("DEFAULT", benchPressId);

        MvcResult created = mockMvc.perform(post("/api/routines")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Push Legs",
                                  "exercises":[
                                    {"exerciseType":"DEFAULT","exerciseId":%d},
                                    {"exerciseType":"CUSTOM","exerciseId":%d}
                                  ]
                                }
                                """.formatted(benchPressId, customId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exercises[0].exerciseName").value("벤치프레스"))
                .andExpect(jsonPath("$.exercises[1].exerciseName").value("나만의 레그 프레스"))
                .andReturn();
        long routineId = json(created).path("id").asLong();

        mockMvc.perform(put("/api/routines/{routineId}", routineId)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Legs & Push",
                                  "exercises":[
                                    {"exerciseType":"DEFAULT","exerciseId":%d},
                                    {"exerciseType":"DEFAULT","exerciseId":%d}
                                  ]
                                }
                                """.formatted(squatId, benchPressId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].exerciseName").value("스쿼트"))
                .andExpect(jsonPath("$.exercises[0].orderIndex").value(1))
                .andExpect(jsonPath("$.exercises[1].exerciseName").value("벤치프레스"));

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
        long exerciseId = findDefaultExerciseId("랫풀다운");

        MvcResult created = mockMvc.perform(post("/api/routines")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Pull",
                                  "exercises":[
                                    {"exerciseType":"DEFAULT","exerciseId":%d}
                                  ]
                                }
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

    private void createCompletedWorkoutWithRecord(
            String exerciseType,
            long exerciseId) throws Exception {
        MvcResult workout = mockMvc.perform(post("/api/workouts")
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workoutDate":"2026-09-17"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long workoutId = json(workout).path("id").asLong();

        MvcResult added = mockMvc.perform(post(
                        "/api/workouts/{workoutId}/exercises", workoutId)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exerciseType":"%s","exerciseId":%d}
                                """.formatted(exerciseType, exerciseId)))
                .andExpect(status().isOk())
                .andReturn();
        long workoutExerciseId =
                json(added).path("exercises").get(0).path("id").asLong();

        mockMvc.perform(post(
                        "/api/workouts/{workoutId}/exercises/{workoutExerciseId}/sets",
                        workoutId, workoutExerciseId)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"weightKg":80,"reps":8,"completed":true}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/workouts/{workoutId}/complete", workoutId)
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk());
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
