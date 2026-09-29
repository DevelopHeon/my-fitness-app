package com.myfitness.ai.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class AiPolicyCorpusTest {
    private static final Path SEED = Path.of("src/test/resources/ai-policy/seed-v0.jsonl");

    @Test
    @DisplayName("24개 초안은 진단용이며 독립 검토 전에는 최종 정답으로 승격하지 못한다")
    void loadsDraftButRejectsUnreviewedGold() throws Exception {
        assertThat(AiPolicyCorpus.load(SEED, false)).hasSize(24);
        assertThatThrownBy(() -> AiPolicyCorpus.load(SEED, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("독립 검토");
    }

    @Test
    @DisplayName("중복 ID와 family 단위 dev/holdout 누수를 차단한다")
    void detectsDuplicatesAndSplitLeakage() throws Exception {
        ArrayList<JsonNode> cases = new ArrayList<>(AiPolicyCorpus.load(SEED, false));
        cases.add(cases.get(0));
        assertThatThrownBy(() -> AiPolicyCorpus.validate(cases, false)).hasMessageContaining("중복");
        cases.removeLast();
        ((ObjectNode) cases.get(9)).put("split", "dev");
        ((ObjectNode) cases.get(23)).put("split", "holdout");
        assertThatThrownBy(() -> AiPolicyCorpus.validate(cases, false)).hasMessageContaining("누수");
    }

    @Test
    @DisplayName("위험 정답이 누락되면 평가를 중단한다")
    void rejectsMissingHazardLabel() throws Exception {
        List<JsonNode> cases = AiPolicyCorpus.load(SEED, false);
        ((ObjectNode) cases.get(0).path("goldHazards")).remove("medical_decision");
        assertThatThrownBy(() -> AiPolicyCorpus.validate(cases, false))
                .hasMessageContaining("위험 정답");
    }
}
