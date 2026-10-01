package com.myfitness.architecture;

import static com.myfitness.test.security.TestSecurity.authenticatedUser;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
class EntityBoundaryApiIntegrationTest {
    private static final long WORKOUT_USER = 9101L;
    private static final long BODY_USER = 9102L;
    private static final long NUTRITION_USER = 9103L;

    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Workout 응답은 요청 트랜잭션 종료 후에도 운동과 세트 projection을 조회할 수 있다")
    void returnsWorkoutProjectionAcrossRequestTransactions() throws Exception {
        long exerciseId = findDefaultExerciseId(WORKOUT_USER, "벤치프레스");

        MvcResult created = mockMvc.perform(post("/api/workouts")
                        .with(authenticatedUser(WORKOUT_USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"workoutDate":"2026-09-20","memo":"boundary"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long workoutId = json(created).path("id").asLong();

        MvcResult added = mockMvc.perform(post(
                        "/api/workouts/{workoutId}/exercises", workoutId)
                        .with(authenticatedUser(WORKOUT_USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exerciseType":"DEFAULT","exerciseId":%d}
                                """.formatted(exerciseId)))
                .andExpect(status().isOk())
                .andReturn();
        long workoutExerciseId =
                json(added).path("exercises").get(0).path("id").asLong();

        mockMvc.perform(post(
                        "/api/workouts/{workoutId}/exercises/{workoutExerciseId}/sets",
                        workoutId,
                        workoutExerciseId)
                        .with(authenticatedUser(WORKOUT_USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"weightKg":70,"reps":8,"completed":true}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/workouts/{workoutId}/complete", workoutId)
                        .with(authenticatedUser(WORKOUT_USER)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/workouts/{workoutId}", workoutId)
                        .with(authenticatedUser(WORKOUT_USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].exerciseName")
                        .value("벤치프레스"))
                .andExpect(jsonPath("$.exercises[0].sets[0].weightKg")
                        .value(70));

        mockMvc.perform(get("/api/workouts")
                        .with(authenticatedUser(WORKOUT_USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].exercises[0].sets[0].reps")
                        .value(8));

        mockMvc.perform(get(
                        "/api/exercises/DEFAULT/{exerciseId}/previous-record",
                        exerciseId)
                        .with(authenticatedUser(WORKOUT_USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workoutId").value(workoutId))
                .andExpect(jsonPath("$.sets[0].weightKg").value(70));
    }

    @Test
    @DisplayName("Body 응답은 요청 트랜잭션 종료 후에도 목록 상세와 추이 projection을 조회할 수 있다")
    void returnsBodyProjectionAcrossRequestTransactions() throws Exception {
        createBodyRecord(
                "72.5",
                "18.5",
                "34.0",
                "2026-09-19T00:00:00Z");
        MvcResult latest = createBodyRecord(
                "72.0",
                "18.0",
                "34.4",
                "2026-09-20T00:00:00Z");
        long latestId = json(latest).path("id").asLong();

        mockMvc.perform(get("/api/body-records")
                        .with(authenticatedUser(BODY_USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(latestId))
                .andExpect(jsonPath("$[0].weightKg").value(72.0));

        mockMvc.perform(get("/api/body-records/{id}", latestId)
                        .with(authenticatedUser(BODY_USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skeletalMuscleKg").value(34.4));

        mockMvc.perform(get("/api/body-records/trend")
                        .with(authenticatedUser(BODY_USER))
                        .param("days", "3650"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latest.id").value(latestId))
                .andExpect(jsonPath("$.changeFromPrevious.weightKg")
                        .value(-0.5));
    }

    @Test
    @DisplayName("Nutrition 응답은 요청 트랜잭션 종료 후에도 음식 식단 목표 projection을 조회할 수 있다")
    void returnsNutritionProjectionAcrossRequestTransactions() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 20);

        mockMvc.perform(put("/api/nutrition-goals/current")
                        .with(authenticatedUser(NUTRITION_USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "calories":2000,
                                  "carbohydrateGrams":250,
                                  "proteinGrams":150,
                                  "fatGrams":60
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/meals/items")
                        .with(authenticatedUser(NUTRITION_USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mealDate":"%s",
                                  "mealType":"DINNER",
                                  "foodName":"Boundary Chicken",
                                  "calories":247.5,
                                  "proteinGrams":46.5
                                }
                                """.formatted(date)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/nutrition-goals/current")
                        .with(authenticatedUser(NUTRITION_USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proteinGrams").value(150));

        mockMvc.perform(get("/api/meals/daily")
                        .with(authenticatedUser(NUTRITION_USER))
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goal.calories").value(2000))
                .andExpect(jsonPath("$.consumed.calories").value(247.5))
                .andExpect(jsonPath("$.meals[2].mealType").value("DINNER"))
                .andExpect(jsonPath("$.meals[2].items[0].foodName")
                        .value("Boundary Chicken"));


    }

    private long findDefaultExerciseId(long userId, String name)
            throws Exception {
        JsonNode exercises = json(mockMvc.perform(get("/api/exercises")
                        .with(authenticatedUser(userId)))
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

    private MvcResult createBodyRecord(
            String weight,
            String bodyFat,
            String muscle,
            String measuredAt) throws Exception {
        return mockMvc.perform(post("/api/body-records")
                        .with(authenticatedUser(BODY_USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weightKg":%s,
                                  "bodyFatPercentage":%s,
                                  "skeletalMuscleKg":%s,
                                  "measuredAt":"%s"
                                }
                                """.formatted(
                                        weight,
                                        bodyFat,
                                        muscle,
                                        measuredAt)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(
                result.getResponse().getContentAsString());
    }
}
