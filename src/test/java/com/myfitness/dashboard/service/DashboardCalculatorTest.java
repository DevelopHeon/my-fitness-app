package com.myfitness.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DashboardCalculatorTest {

    @Test
    @DisplayName("세트 볼륨은 중량과 반복 횟수를 곱해 계산한다")
    void calculatesSetVolume() {
        BigDecimal volume = DashboardCalculator.volume(
                new BigDecimal("67.5"), 8);

        assertThat(volume).isEqualByComparingTo("540.00");
    }

    @Test
    @DisplayName("Epley 공식으로 추정 1RM을 소수 둘째 자리까지 계산한다")
    void calculatesEstimatedOneRepMax() {
        BigDecimal oneRepMax = DashboardCalculator.estimatedOneRepMax(
                new BigDecimal("100"), 10);

        assertThat(oneRepMax).isEqualByComparingTo("133.33");
    }

    @Test
    @DisplayName("이전 기간 대비 볼륨 증감률을 백분율로 계산한다")
    void calculatesVolumeChangePercentage() {
        BigDecimal change = DashboardCalculator.changePercentage(
                new BigDecimal("1200"),
                new BigDecimal("1000"));

        assertThat(change).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("이전 기간 볼륨이 0이면 증감률은 제공하지 않는다")
    void omitsChangePercentageWithoutPreviousVolume() {
        assertThat(DashboardCalculator.changePercentage(
                new BigDecimal("1200"),
                BigDecimal.ZERO)).isNull();
    }
}
