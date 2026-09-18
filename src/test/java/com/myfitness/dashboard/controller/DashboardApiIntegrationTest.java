package com.myfitness.dashboard.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
class DashboardApiIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("Dashboard에서 운동 빈도와 Volume, 신체 변화, 종목 PR과 추이를 함께 조회한다")
    void returnsWorkoutBodyAndExerciseDashboardMetrics() throws Exception {
        LocalDate today = LocalDate.now();
        long benchPressId = findDefaultExerciseId("벤치프레스");

        createCompletedWorkout(today.minusDays(2), benchPressId, 100, 10, 1L);
        createCompletedWorkout(today.minusDays(9), benchPressId, 80, 10, 1L);

        Instant now = Instant.now();
        createBodyRecord(
                new BigDecimal("72.0"),
                new BigDecimal("18.0"),
                new BigDecimal("34.0"),
                now.minusSeconds(86_400L * 5),
                1L);
        createBodyRecord(
                new BigDecimal("71.0"),
                new BigDecimal("17.5"),
                new BigDecimal("34.5"),
                now.minusSeconds(86_400L),
                1L);

        mockMvc.perform(get("/api/dashboard")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workout.last7DaysWorkoutCount").value(1))
                .andExpect(jsonPath("$.workout.last30DaysWorkoutCount").value(2))
                .andExpect(jsonPath("$.workout.last7DaysVolume").value(1000))
                .andExpect(jsonPath("$.workout.previous7DaysVolume").value(800))
                .andExpect(jsonPath("$.workout.last7DaysVolumeChangePercentage").value(25))
                .andExpect(jsonPath("$.body.latest.weightKg").value(71))
                .andExpect(jsonPath("$.body.changeFromPrevious.weightKg").value(-1))
                .andExpect(jsonPath("$.body.changeFromPrevious.bodyFatPercentage").value(-0.5))
                .andExpect(jsonPath("$.body.changeFromPrevious.skeletalMuscleKg").value(0.5))
                .andExpect(jsonPath("$.exercises[0].exerciseName").value("벤치프레스"))
                .andExpect(jsonPath("$.exercises[0].maxWeightKg").value(100))
                .andExpect(jsonPath("$.exercises[0].maxEstimatedOneRepMax").value(133.33))
                .andExpect(jsonPath("$.exercises[0].recentRecords.length()").value(2));
    }

    @Test
    @DisplayName("Dashboard 집계에는 진행 중 Workout과 다른 사용자의 기록이 포함되지 않는다")
    void excludesInProgressAndOtherUsersFromDashboard() throws Exception {
        LocalDate today = LocalDate.now();
        long benchPressId = findDefaultExerciseId("벤치프레스");

        createCompletedWorkout(today.minusDays(1), benchPressId, 60, 10, 1L);
        createInProgressWorkout(today, benchPressId, 300, 10, 1L);
        createCompletedWorkout(today.minusDays(1), benchPressId, 200, 10, 2L);

        mockMvc.perform(get("/api/dashboard")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workout.last7DaysWorkoutCount").value(1))
                .andExpect(jsonPath("$.workout.last7DaysVolume").value(600))
                .andExpect(jsonPath("$.exercises[0].maxWeightKg").value(60));
    }

    private void createCompletedWorkout(
            LocalDate date,
            long exerciseId,
            int weightKg,
            int reps,
            long userId) throws Exception {
        MvcResult workoutCreated = mockMvc.perform(post("/api/workouts")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workoutDate":"%s"}
                                """.formatted(date)))
                .andExpect(status().isCreated())
                .andReturn();
        long workoutId = json(workoutCreated).path("id").asLong();

        MvcResult exerciseAdded = mockMvc.perform(post(
                        "/api/workouts/{workoutId}/exercises", workoutId)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exerciseType":"DEFAULT","exerciseId":%d}
                                """.formatted(exerciseId)))
                .andExpect(status().isOk())
                .andReturn();
        long workoutExerciseId = json(exerciseAdded)
                .path("exercises").get(0).path("id").asLong();

        mockMvc.perform(post(
                        "/api/workouts/{workoutId}/exercises/{workoutExerciseId}/sets",
                        workoutId, workoutExerciseId)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"weightKg":%d,"reps":%d,"completed":true}
                                """.formatted(weightKg, reps)))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/workouts/{workoutId}/complete", workoutId)
                        .header("X-User-Id", userId))
                .andExpect(status().isOk());
    }

    private void createInProgressWorkout(
            LocalDate date,
            long exerciseId,
            int weightKg,
            int reps,
            long userId) throws Exception {
        MvcResult workoutCreated = mockMvc.perform(post("/api/workouts")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workoutDate":"%s"}
                                """.formatted(date)))
                .andExpect(status().isCreated())
                .andReturn();
        long workoutId = json(workoutCreated).path("id").asLong();

        MvcResult exerciseAdded = mockMvc.perform(post(
                        "/api/workouts/{workoutId}/exercises", workoutId)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exerciseType":"DEFAULT","exerciseId":%d}
                                """.formatted(exerciseId)))
                .andExpect(status().isOk())
                .andReturn();
        long workoutExerciseId = json(exerciseAdded)
                .path("exercises").get(0).path("id").asLong();

        mockMvc.perform(post(
                        "/api/workouts/{workoutId}/exercises/{workoutExerciseId}/sets",
                        workoutId, workoutExerciseId)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"weightKg":%d,"reps":%d,"completed":true}
                                """.formatted(weightKg, reps)))
                .andExpect(status().isOk());
    }

    private void createBodyRecord(
            BigDecimal weightKg,
            BigDecimal bodyFatPercentage,
            BigDecimal skeletalMuscleKg,
            Instant measuredAt,
            long userId) throws Exception {
        mockMvc.perform(post("/api/body-records")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weightKg":%s,
                                  "bodyFatPercentage":%s,
                                  "skeletalMuscleKg":%s,
                                  "measuredAt":"%s",
                                  "memo":null
                                }
                                """.formatted(
                                        weightKg,
                                        bodyFatPercentage,
                                        skeletalMuscleKg,
                                        measuredAt)))
                .andExpect(status().isCreated());
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
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
