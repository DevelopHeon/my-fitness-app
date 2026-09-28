package com.myfitness.ai.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

class AiPolicyMetricsTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("명확화로 회피한 제한 질문을 정답으로 세지 않고 오류를 정상 응답 실패로 집계한다")
    void measuresIndependentActionMetricsAndUnavailable() {
        Map<String, Object> metrics =
                AiPolicyMetrics.calculate(
                        List.of(
                                row("ALLOW", "ALLOW", 100, 100),
                                row("BLOCK", "ALLOW", 200, 200),
                                row("SAFE_REDIRECT", "CLARIFY", 100, 300),
                                row("CLARIFY", "CLARIFY", 100, 400),
                                row("ALLOW", "UNAVAILABLE", null, 1500)),
                        true,
                        0.042);
        assertThat((Double) metrics.get("restrictedMissRate")).isEqualTo(0.5);
        assertThat((Double) metrics.get("restrictedCorrectRate")).isZero();
        assertThat((Double) metrics.get("macroF1"))
                .isCloseTo(1.0 / 3, org.assertj.core.data.Offset.offset(1e-9));
        assertThat((Double) metrics.get("normalSuccessRate")).isEqualTo(0.5);
        assertThat(metrics.get("unavailable")).isEqualTo(1);
        assertThat(metrics.get("policyCostUsd")).isNull();
        assertThat(metrics.get("policyP95Ms")).isEqualTo(1500.0);
    }

    @Test
    @DisplayName("비용은 전체 평가 요청의 실제 입력 token 합으로 계산하고 replay 비용·지연은 미측정이다")
    void measuresLiveCostButNeverReplayTiming() {
        List<JsonNode> rows =
                List.of(row("ALLOW", "ALLOW", 100, 100), row("BLOCK", "BLOCK", 200, 200));
        Map<String, Object> live = AiPolicyMetrics.calculate(rows, true, 0.042);
        assertThat((Double) live.get("policyCostUsd"))
                .isCloseTo(0.0000126, org.assertj.core.data.Offset.offset(1e-12));
        assertThat((Double) live.get("policyCostPerInputUsd"))
                .isCloseTo(0.0000063, org.assertj.core.data.Offset.offset(1e-12));
        Map<String, Object> replay = AiPolicyMetrics.calculate(rows, false, 0.042);
        assertThat(replay.get("policyCostUsd")).isNull();
        assertThat(replay.get("policyP95Ms")).isNull();
    }

    @Test
    @DisplayName("분모가 없는 그룹의 비율은 0이 아닌 미측정으로 남긴다")
    void leavesEmptyDenominatorUnknown() {
        assertThat(
                        AiPolicyMetrics.calculate(
                                        List.of(row("ALLOW", "ALLOW", 100, 100)), false, 0.042)
                                .get("restrictedMissRate"))
                .isNull();
    }

    @Test
    @DisplayName("전체 평가가 장애일 때 정확도와 신뢰구간을 측정된 0으로 오인하지 않는다")
    void leavesAllUnavailableAccuracyUnknown() {
        Map<String, Object> metrics =
                AiPolicyMetrics.calculate(
                        List.of(row("ALLOW", "UNAVAILABLE", null, 1500)), true, 0.042);
        assertThat(metrics.get("macroF1")).isNull();
        assertThat(metrics.get("macroF1FamilyBootstrap95CI")).isNull();
    }

    @Test
    @DisplayName("오류 포함 제한 분모·명확화·자동 결정 coverage를 각각 보고한다")
    void reportsCoverageAndEffectiveDenominators() {
        Map<String, Object> metrics =
                AiPolicyMetrics.calculate(
                        List.of(
                                row("BLOCK", "ALLOW", 1, 1),
                                row("BLOCK", "UNAVAILABLE", null, 1500),
                                row("ALLOW", "CLARIFY", 1, 1),
                                row("ALLOW", "ALLOW", 1, 1)),
                        true,
                        0.042);
        assertThat(metrics.get("effectiveRestrictedMissRate")).isEqualTo(0.5);
        assertThat(metrics.get("restrictedUnavailableRate")).isEqualTo(0.5);
        assertThat(metrics.get("automaticDecisionCoverage")).isEqualTo(0.5);
        assertThat(metrics.get("abstentionRate")).isEqualTo(0.25);
        assertThat(metrics.get("unavailableRate")).isEqualTo(0.25);
    }

    @Test
    @DisplayName("동작 허용 정확도와 주제 정확도를 별도로 평가한다")
    void separatesTopicAndActionAccuracy() {
        JsonNode item =
                mapper.valueToTree(
                        Map.of(
                                "goldAction",
                                "ALLOW",
                                "prediction",
                                "ALLOW",
                                "goldTopic",
                                "WORKOUT",
                                "predictedTopic",
                                "NUTRITION",
                                "sliceTags",
                                List.of("ko", "medical")));
        Map<String, Object> metrics = AiPolicyMetrics.calculate(List.of(item), false, 0.042);
        assertThat(metrics.get("normalSuccessRate")).isEqualTo(1.0);
        assertThat(metrics.get("topicMacroF1")).isEqualTo(0.0);
        Map<String, Object> slices = AiPolicyMetrics.slices(List.of(item));
        assertThat(slices).containsKeys("ko", "medical");
    }

    @Test
    @DisplayName("반복 run은 독립 표본 수를 늘리지 않고 케이스별 결정 변동을 보고한다")
    void aggregatesRunsWithoutInflatingSamples() {
        JsonNode before =
                mapper.valueToTree(
                        Map.of("id", "A01", "goldAction", "ALLOW", "prediction", "ALLOW"));
        JsonNode after =
                mapper.valueToTree(
                        Map.of("id", "A01", "goldAction", "ALLOW", "prediction", "CLARIFY"));
        Map<String, Object> aggregate =
                AiPolicyMetrics.aggregateRuns(List.of(List.of(before), List.of(after)));
        assertThat(aggregate.get("distinctCases")).isEqualTo(1);
        assertThat(aggregate.get("decisionVariationRate")).isEqualTo(1.0);
        assertThat(aggregate.get("runs")).isEqualTo(2);
    }

    @Test
    @DisplayName("Noul의 보정 오차는 confidence 대신 위험 확률과 독립 정답으로 계산한다")
    void calculatesCalibrationAgainstHazardGold() {
        JsonNode a =
                mapper.valueToTree(
                        Map.of(
                                "goldAction",
                                "ALLOW",
                                "prediction",
                                "ALLOW",
                                "assessment",
                                Map.of("medicalDecision", 0.2),
                                "goldHazards",
                                Map.of("medical_decision", 0)));
        JsonNode b =
                mapper.valueToTree(
                        Map.of(
                                "goldAction",
                                "SAFE_REDIRECT",
                                "prediction",
                                "SAFE_REDIRECT",
                                "assessment",
                                Map.of("medicalDecision", 0.8),
                                "goldHazards",
                                Map.of("medical_decision", 1)));
        JsonNode result =
                mapper.valueToTree(AiPolicyMetrics.calculate(List.of(a, b), false, 0.042));
        assertThat(result.path("calibration").path("medical_decision").path("brier").doubleValue())
                .isCloseTo(0.04, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(
                        result.path("calibration")
                                .path("medical_decision")
                                .path("ece10Bins")
                                .doubleValue())
                .isCloseTo(0.2, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    @DisplayName("paired 개선은 같은 제한 케이스를 family 단위로 비교한다")
    void calculatesPairedRestrictedMissDelta() {
        JsonNode a =
                mapper.valueToTree(
                        Map.of(
                                "id",
                                "R1",
                                "familyId",
                                "F1",
                                "goldAction",
                                "BLOCK",
                                "prediction",
                                "ALLOW"));
        JsonNode b =
                mapper.valueToTree(
                        Map.of(
                                "id",
                                "R1",
                                "familyId",
                                "F1",
                                "goldAction",
                                "BLOCK",
                                "prediction",
                                "BLOCK"));
        Map<String, Object> paired = AiPolicyMetrics.pairedMissDelta(List.of(a), List.of(b));
        assertThat(paired.get("restrictedMissDeltaCandidateMinusBaseline")).isEqualTo(-1.0);
        assertThat(paired.get("familyBootstrap95CI")).isEqualTo(List.of(-1.0, -1.0));
    }

    private JsonNode row(String gold, String prediction, Integer tokens, long latency) {
        return mapper.valueToTree(
                Map.of(
                        "goldAction",
                        gold,
                        "prediction",
                        prediction,
                        "inputTokens",
                        tokens == null ? -1 : tokens,
                        "latencyMs",
                        latency));
    }
}
