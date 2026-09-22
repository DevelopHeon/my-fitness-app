package com.myfitness.body.presentation.controller;

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
class BodyRecordApiIntegrationTest {
    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("같은 날 여러 신체 기록을 생성하고 최신순 조회와 변화량을 확인한다")
    void createsMultipleRecordsPerDayAndReadsTrend() throws Exception {
        createRecord(
                1L,
                "72.80",
                "19.20",
                "33.90",
                "2026-09-18T00:00:00Z",
                "아침");
        MvcResult latest = createRecord(
                1L,
                "72.30",
                "18.90",
                "34.10",
                "2026-09-18T06:00:00Z",
                "오후");
        long latestId = json(latest).path("id").asLong();

        mockMvc.perform(get("/api/body-records")
                        .header("X-User-Id", 1L)
                        .param("from", "2026-09-17T00:00:00Z")
                        .param("to", "2026-09-19T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(latestId))
                .andExpect(jsonPath("$[0].weightKg").value(72.3))
                .andExpect(jsonPath("$[1].weightKg").value(72.8));

        mockMvc.perform(get("/api/body-records/trend")
                        .header("X-User-Id", 1L)
                        .param("days", "3650"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latest.id").value(latestId))
                .andExpect(jsonPath("$.changeFromPrevious.weightKg").value(-0.5))
                .andExpect(jsonPath("$.changeFromPrevious.bodyFatPercentage").value(-0.3))
                .andExpect(jsonPath("$.changeFromPrevious.skeletalMuscleKg").value(0.2));
    }

    @Test
    @DisplayName("신체 기록을 수정 및 삭제하고 다른 사용자의 접근을 차단한다")
    void updatesDeletesAndProtectsBodyRecordOwnership() throws Exception {
        MvcResult created = createRecord(
                1L,
                "72.80",
                "19.20",
                "33.90",
                "2026-09-18T00:00:00Z",
                null);
        long recordId = json(created).path("id").asLong();

        mockMvc.perform(put("/api/body-records/{bodyRecordId}", recordId)
                        .header("X-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weightKg":72.10,
                                  "bodyFatPercentage":18.70,
                                  "skeletalMuscleKg":34.30,
                                  "measuredAt":"2026-09-18T01:00:00Z",
                                  "memo":"수정 기록"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weightKg").value(72.1))
                .andExpect(jsonPath("$.memo").value("수정 기록"));

        mockMvc.perform(get("/api/body-records/{bodyRecordId}", recordId)
                        .header("X-User-Id", 2L))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/body-records/{bodyRecordId}", recordId)
                        .header("X-User-Id", 1L))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/body-records/{bodyRecordId}", recordId)
                        .header("X-User-Id", 1L))
                .andExpect(status().isNotFound());
    }

    private MvcResult createRecord(
            long userId,
            String weightKg,
            String bodyFatPercentage,
            String skeletalMuscleKg,
            String measuredAt,
            String memo) throws Exception {
        String memoJson = memo == null ? "null" : "\"" + memo + "\"";
        return mockMvc.perform(post("/api/body-records")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "weightKg":%s,
                                  "bodyFatPercentage":%s,
                                  "skeletalMuscleKg":%s,
                                  "measuredAt":"%s",
                                  "memo":%s
                                }
                                """.formatted(
                                        weightKg,
                                        bodyFatPercentage,
                                        skeletalMuscleKg,
                                        measuredAt,
                                        memoJson)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
