package com.myfitness.body.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.myfitness.body.domain.exception.BodyRecordRuleException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BodyRecordTest {
    private final Instant now = Instant.parse("2026-09-18T08:00:00Z");

    @Test
    @DisplayName("체중, 체지방률, 골격근량과 측정 시각을 저장한다")
    void createsBodyRecordWithMeasurements() {
        BodyRecord record = BodyRecord.create(
                1L,
                new BigDecimal("72.40"),
                new BigDecimal("18.50"),
                new BigDecimal("34.20"),
                now.minusSeconds(3600),
                "아침 측정",
                now);

        assertThat(record.getWeightKg()).isEqualByComparingTo("72.40");
        assertThat(record.getBodyFatPercentage()).isEqualByComparingTo("18.50");
        assertThat(record.getSkeletalMuscleKg()).isEqualByComparingTo("34.20");
        assertThat(record.getMemo()).isEqualTo("아침 측정");
    }

    @Test
    @DisplayName("체중과 골격근량은 양수이고 체지방률은 0에서 100 사이여야 한다")
    void rejectsInvalidBodyMeasurements() {
        assertThatThrownBy(() -> BodyRecord.create(
                1L,
                BigDecimal.ZERO,
                new BigDecimal("18"),
                new BigDecimal("34"),
                now,
                null,
                now))
                .isInstanceOf(BodyRecordRuleException.class)
                .hasMessageContaining("체중");

        assertThatThrownBy(() -> BodyRecord.create(
                1L,
                new BigDecimal("72"),
                new BigDecimal("101"),
                new BigDecimal("34"),
                now,
                null,
                now))
                .isInstanceOf(BodyRecordRuleException.class)
                .hasMessageContaining("체지방률");

        assertThatThrownBy(() -> BodyRecord.create(
                1L,
                new BigDecimal("72"),
                new BigDecimal("18"),
                BigDecimal.ZERO,
                now,
                null,
                now))
                .isInstanceOf(BodyRecordRuleException.class)
                .hasMessageContaining("골격근량");

        assertThatThrownBy(() -> BodyRecord.create(
                1L,
                new BigDecimal("72"),
                new BigDecimal("18"),
                new BigDecimal("34"),
                now.plusSeconds(60),
                null,
                now))
                .isInstanceOf(BodyRecordRuleException.class)
                .hasMessageContaining("미래 시각");
    }
}
