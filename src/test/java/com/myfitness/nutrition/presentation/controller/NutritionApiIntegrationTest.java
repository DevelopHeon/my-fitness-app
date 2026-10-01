package com.myfitness.nutrition.presentation.controller;

import static com.myfitness.test.security.TestSecurity.authenticatedUser;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@Transactional
class NutritionApiIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;
    private MockMvc mockMvc;
    private final LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void recordsWithoutCatalogOrGoalAndKeepsUnknownMacros() throws Exception {
        create(1L, payload(today, "LUNCH", "  비빔밥  ", "500", "null"));
        create(1L, payload(today, "LUNCH", "비빔밥", "200", "0"));
        mockMvc.perform(get("/api/meals/daily").with(authenticatedUser(1L)).param("date", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consumed.calories").value(700))
                .andExpect(jsonPath("$.consumed.proteinGrams").isEmpty())
                .andExpect(jsonPath("$.consumed.carbohydrateGrams").value(0))
                .andExpect(jsonPath("$.goal").isEmpty())
                .andExpect(jsonPath("$.meals[1].items[0].foodName").value("비빔밥"));
    }

    @Test
    void movesEditsClearsAndDeletesOwnedItem() throws Exception {
        long id = create(1L, payload(today, "LUNCH", "밥", "500", "20"));
        String update = payload(today.minusDays(1), "DINNER", "현미밥", "300", "null");
        mockMvc.perform(patch("/api/meals/items/{id}", id).with(authenticatedUser(1L))
                        .contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mealDate").value(today.minusDays(1).toString()))
                .andExpect(jsonPath("$.calories").value(300))
                .andExpect(jsonPath("$.proteinGrams").isEmpty());
        mockMvc.perform(get("/api/meals/daily").with(authenticatedUser(1L)).param("date", today.toString()))
                .andExpect(jsonPath("$.consumed.calories").value(0));
        mockMvc.perform(patch("/api/meals/items/{id}", id).with(authenticatedUser(2L))
                        .contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/meals/items/{id}", id).with(authenticatedUser(2L)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/meals/items/{id}", id).with(authenticatedUser(1L)))
                .andExpect(status().isNoContent());
    }

    @Test
    void calculatesOnlyKnownRemainingNutrients() throws Exception {
        mockMvc.perform(put("/api/nutrition-goals/current").with(authenticatedUser(1L))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"calories":2000,"carbohydrateGrams":250,"proteinGrams":150,"fatGrams":60}
                        """))
                .andExpect(status().isOk());
        create(1L, payload(today, "BREAKFAST", "밥", "2100", "null"));
        mockMvc.perform(get("/api/meals/daily").with(authenticatedUser(1L)).param("date", today.toString()))
                .andExpect(jsonPath("$.remaining.calories").value(-100))
                .andExpect(jsonPath("$.remaining.proteinGrams").isEmpty())
                .andExpect(jsonPath("$.remaining.carbohydrateGrams").value(250));
    }

    @Test
    void rejectsInvalidInputsAndFutureDates() throws Exception {
        for (String body : new String[] {
                payload(today.plusDays(1), "LUNCH", "밥", "100", "null"),
                payload(today, "LUNCH", " ", "100", "null"),
                payload(today, "LUNCH", "밥", "-1", "null"),
                payload(today, "LUNCH", "밥", "100.001", "null"),
                payload(today, "LUNCH", "밥", "1000000", "null"),
                payload(today, "LUNCH", "밥", "100", "-1")}) {
            mockMvc.perform(post("/api/meals/items").with(authenticatedUser(1L))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void recordsMultipleFoodsWithOneDateAndMealType() throws Exception {
        mockMvc.perform(post("/api/meals/items/batch").with(authenticatedUser(1L))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"mealDate":"%s","mealType":"LUNCH","items":[
                          {"foodName":"밥","calories":300},
                          {"foodName":"닭가슴살","calories":200,"proteinGrams":30}]}
                        """.formatted(today)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].foodName").value("밥"))
                .andExpect(jsonPath("$[1].proteinGrams").value(30));
        mockMvc.perform(get("/api/meals/daily").with(authenticatedUser(1L)).param("date", today.toString()))
                .andExpect(jsonPath("$.consumed.calories").value(500))
                .andExpect(jsonPath("$.consumed.proteinGrams").isEmpty());
    }

    @Test
    void invalidBatchDoesNotRecordAnyFood() throws Exception {
        for (String items : new String[] {"[]", "[null]",
                "[{\"foodName\":\"밥\",\"calories\":300},{\"foodName\":\" \",\"calories\":200}]"}) {
            mockMvc.perform(post("/api/meals/items/batch").with(authenticatedUser(1L))
                            .contentType(MediaType.APPLICATION_JSON).content("""
                            {"mealDate":"%s","mealType":"LUNCH","items":%s}
                            """.formatted(today, items)))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(get("/api/meals/daily").with(authenticatedUser(1L)).param("date", today.toString()))
                .andExpect(jsonPath("$.consumed.calories").value(0));
    }

    @Test
    void calendarIncludesOnlyOwnedFoodsInMonthAndKeepsRecordedZero() throws Exception {
        LocalDate monthStart = today.withDayOfMonth(1);
        create(1L, payload(monthStart, "LUNCH", "밥", "300", "null"));
        create(1L, payload(monthStart, "LUNCH", "닭", "200", "null"));
        create(1L, payload(monthStart, "DINNER", "차", "0", "null"));
        create(1L, payload(monthStart.minusDays(1), "LUNCH", "지난달", "500", "null"));
        create(2L, payload(monthStart, "BREAKFAST", "다른 사용자", "900", "null"));
        long removed = create(1L, payload(monthStart, "SNACK", "삭제", "100", "null"));
        mockMvc.perform(delete("/api/meals/items/{id}", removed).with(authenticatedUser(1L)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/meals/calendar").with(authenticatedUser(1L))
                        .param("month", monthStart.toString().substring(0, 7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].date").value(monthStart.toString()))
                .andExpect(jsonPath("$[0].calories").value(500))
                .andExpect(jsonPath("$[0].mealTypes.length()").value(2))
                .andExpect(jsonPath("$[0].mealTypes[0]").value("LUNCH"))
                .andExpect(jsonPath("$[0].mealTypes[1]").value("DINNER"));
        mockMvc.perform(get("/api/meals/calendar").with(authenticatedUser(1L)).param("month", "invalid"))
                .andExpect(status().isBadRequest());
    }

    private long create(long userId, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/meals/items").with(authenticatedUser(userId))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    private String payload(LocalDate date, String type, String name, String calories, String protein) {
        return """
                {"mealDate":"%s","mealType":"%s","foodName":"%s","calories":%s,
                 "carbohydrateGrams":0,"proteinGrams":%s,"fatGrams":null}
                """.formatted(date, type, name, calories, protein);
    }
}
