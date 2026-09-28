package com.myfitness.dashboard.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myfitness.dashboard.application.port.out.DashboardDataPort;
import com.myfitness.dashboard.application.port.out.DashboardDataPort.BodyData;
import com.myfitness.dashboard.application.port.out.DashboardDataPort.DashboardSourceData;
import com.myfitness.dashboard.application.result.DashboardResult.BodySummary;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

class DashboardBodyOrderingTest {
    private static final Instant MEASURED_AT = Instant.parse("2026-09-18T13:37:00Z");

    @Test
    @DisplayName("동일 측정 일시의 기존 데이터는 높은 ID를 최신 Body 기록으로 판단한다")
    void prefersHigherIdWhenBodyMeasurementTimeIsEqual() {
        DashboardDataPort port =
                userId ->
                        new DashboardSourceData(
                                List.of(),
                                List.of(
                                        new BodyData(
                                                1L,
                                                MEASURED_AT,
                                                new BigDecimal("64"),
                                                new BigDecimal("15"),
                                                new BigDecimal("31")),
                                        new BodyData(
                                                2L,
                                                MEASURED_AT,
                                                new BigDecimal("65"),
                                                new BigDecimal("18"),
                                                new BigDecimal("32"))));

        DashboardService service =
                new DashboardService(
                        port, Clock.fixed(Instant.parse("2026-09-22T00:00:00Z"), ZoneOffset.UTC));

        BodySummary body = service.getDashboard(1L).body();

        assertThat(body.latest().weightKg()).isEqualByComparingTo("65");
        assertThat(body.changeFromPrevious().weightKg()).isEqualByComparingTo("1");
        assertThat(body.changeFromPrevious().bodyFatPercentage()).isEqualByComparingTo("3");
        assertThat(body.changeFromPrevious().skeletalMuscleKg()).isEqualByComparingTo("1");
        assertThat(body.history())
                .extracting(point -> point.weightKg())
                .containsExactly(new BigDecimal("64"), new BigDecimal("65"));
    }
}
