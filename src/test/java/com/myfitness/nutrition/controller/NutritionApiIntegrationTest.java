package com.myfitness.nutrition.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@Transactional
class NutritionApiIntegrationTest {

    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("음식을 식단에 추가하면 일별 칼로리와 탄단지, 목표 대비 남은 양을 계산한다")
    void recordsMealAndCalculatesDailyNutritionSummary() throws Exception {
        LocalDate date = LocalDate.now();
        long chickenId = createFood(
                1L, "닭가슴살", 100, "G",
                165, 0, 31, 3.6);
        long riceId = createFood(
                1L, "현미밥", 210, "G",
                310, 67, 6, 2);

        mockMvc.perform(put("/api/nutrition-goals/current")
                        .header("X-User-Id", 1L)
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

        addMealFood(1L, date, "BREAKFAST", chickenId, 1.5);
        addMealFood(1L, date, "LUNCH", riceId, 1);

        mockMvc.perform(get("/api/meals/daily")
                        .header("X-User-Id", 1L)
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(date.toString()))
                .andExpect(jsonPath("$.consumed.calories").value(557.5))
                .andExpect(jsonPath("$.consumed.carbohydrateGrams").value(67))
                .andExpect(jsonPath("$.consumed.proteinGrams").value(52.5))
                .andExpect(jsonPath("$.consumed.fatGrams").value(7.4))
                .andExpect(jsonPath("$.goal.calories").value(2000))
                .andExpect(jsonPath("$.remaining.calories").value(1442.5))
                .andExpect(jsonPath("$.remaining.proteinGrams").value(97.5))
                .andExpect(jsonPath("$.meals.length()").value(4))
                .andExpect(jsonPath("$.meals[0].mealType").value("BREAKFAST"))
                .andExpect(jsonPath("$.meals[0].items.length()").value(1))
                .andExpect(jsonPath("$.meals[1].mealType").value("LUNCH"))
                .andExpect(jsonPath("$.meals[1].items.length()").value(1));
    }

    @Test
    @DisplayName("음식 수정과 삭제 이후에도 과거 식단은 스냅샷 영양값을 유지한다")
    void keepsMealSnapshotAfterFoodUpdateAndDelete() throws Exception {
        LocalDate date = LocalDate.now().minusDays(1);
        long foodId = createFood(
                1L, "오트밀", 50, "G",
                190, 32, 7, 4);

        addMealFood(1L, date, "BREAKFAST", foodId, 2);

        mockMvc.perform(put("/api/foods/{foodId}", foodId)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"오트밀 수정",
                                  "servingAmount":60,
                                  "servingUnit":"G",
                                  "calories":250,
                                  "carbohydrateGrams":40,
                                  "proteinGrams":10,
                                  "fatGrams":6
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/foods/{foodId}", foodId)
                        .header("X-User-Id", 1L))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/meals/daily")
                        .header("X-User-Id", 1L)
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consumed.calories").value(380))
                .andExpect(jsonPath("$.meals[0].items[0].foodName").value("오트밀"))
                .andExpect(jsonPath("$.meals[0].items[0].caloriesPerServing").value(190));
    }

    @Test
    @DisplayName("최근 음식과 자주 먹는 음식은 사용자의 식단 기록을 기준으로 재사용 목록을 제공한다")
    void returnsRecentAndFrequentFoodSuggestions() throws Exception {
        LocalDate today = LocalDate.now();
        long chickenId = createFood(
                1L, "닭가슴살", 100, "G",
                165, 0, 31, 3.6);
        long bananaId = createFood(
                1L, "바나나", 1, "COUNT",
                105, 27, 1.3, 0.4);

        addMealFood(1L, today.minusDays(2), "BREAKFAST", chickenId, 1);
        addMealFood(1L, today.minusDays(1), "LUNCH", chickenId, 1);
        addMealFood(1L, today, "SNACK", bananaId, 1);

        mockMvc.perform(get("/api/foods/suggestions")
                        .header("X-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recent[0].name").value("바나나"))
                .andExpect(jsonPath("$.frequent[0].name").value("닭가슴살"));

        mockMvc.perform(get("/api/foods")
                        .header("X-User-Id", 1L)
                        .param("query", "닭"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(chickenId))
                .andExpect(jsonPath("$[0].name").value("닭가슴살"));
    }

    @Test
    @DisplayName("다른 사용자의 음식과 식단 항목은 수정하거나 삭제할 수 없다")
    void protectsNutritionOwnership() throws Exception {
        LocalDate date = LocalDate.now();
        long foodId = createFood(
                1L, "그릭요거트", 150, "G",
                120, 10, 15, 2);
        long itemId = addMealFood(1L, date, "SNACK", foodId, 1);

        mockMvc.perform(put("/api/foods/{foodId}", foodId)
                        .header("X-User-Id", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"변경",
                                  "servingAmount":100,
                                  "servingUnit":"G",
                                  "calories":100,
                                  "carbohydrateGrams":10,
                                  "proteinGrams":10,
                                  "fatGrams":1
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/meals/items/{itemId}", itemId)
                        .header("X-User-Id", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"servings":2}
                                """))
                .andExpect(status().isForbidden());
    }

    private long createFood(
            long userId,
            String name,
            double servingAmount,
            String servingUnit,
            double calories,
            double carbohydrateGrams,
            double proteinGrams,
            double fatGrams) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/foods")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"%s",
                                  "servingAmount":%s,
                                  "servingUnit":"%s",
                                  "calories":%s,
                                  "carbohydrateGrams":%s,
                                  "proteinGrams":%s,
                                  "fatGrams":%s
                                }
                                """.formatted(
                                        name,
                                        servingAmount,
                                        servingUnit,
                                        calories,
                                        carbohydrateGrams,
                                        proteinGrams,
                                        fatGrams)))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).path("id").asLong();
    }

    private long addMealFood(
            long userId,
            LocalDate date,
            String mealType,
            long foodId,
            double servings) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/meals/items")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mealDate":"%s",
                                  "mealType":"%s",
                                  "foodId":%d,
                                  "servings":%s
                                }
                                """.formatted(date, mealType, foodId, servings)))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).path("id").asLong();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
