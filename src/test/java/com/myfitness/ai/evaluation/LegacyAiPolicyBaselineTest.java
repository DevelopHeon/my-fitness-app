package com.myfitness.ai.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.myfitness.ai.application.command.AiClientContext;
import com.myfitness.ai.domain.model.AiQueryType;
import com.myfitness.ai.evaluation.legacy.LegacyAiQueryRouter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

class LegacyAiPolicyBaselineTest {
    @Test
    @DisplayName("원본 legacy Router와 같은 seed에서 변경 전 9/10 누락·2/10 오차단을 재현한다")
    void reproducesFrozenBaseline() throws Exception {
        LegacyAiQueryRouter router = new LegacyAiQueryRouter();
        int misses = 0;
        int falseBlocks = 0;

        for (JsonNode item :
                AiPolicyCorpus.load(Path.of("src/test/resources/ai-policy/seed-v0.jsonl"), false)) {
            String screen = item.path("screen").isNull() ? null : item.path("screen").asText();
            String previous =
                    item.path("previousType").isNull() ? null : item.path("previousType").asText();
            AiQueryType type =
                    router.route(
                            item.path("currentQuestion").asText(),
                            new AiClientContext(screen, null, null),
                            previous == null ? null : AiQueryType.valueOf(previous));
            assertThat(type.name())
                    .as(item.path("id").asText())
                    .isEqualTo(item.path("baselineQueryType").asText());
            String action = type == AiQueryType.OUT_OF_SCOPE ? "BLOCK" : "ALLOW";
            assertThat(action).isEqualTo(item.path("baselineGateAction").asText());
            if (item.path("goldAction").asText().equals("ALLOW") && action.equals("BLOCK"))
                falseBlocks++;
            if ((item.path("goldAction").asText().equals("BLOCK")
                            || item.path("goldAction").asText().equals("SAFE_REDIRECT"))
                    && action.equals("ALLOW")) misses++;
        }
        assertThat(misses).isEqualTo(9);
        assertThat(falseBlocks).isEqualTo(2);
        JsonNode manifest =
                new ObjectMapper()
                        .readTree(
                                Files.readString(
                                        Path.of(
                                                "src/test/resources/ai-policy/manifest-seed-v0.json")));
        assertThat(hash(Path.of("src/test/resources/ai-policy/seed-v0.jsonl")))
                .isEqualTo(manifest.path("datasetSha256").asText());
        assertThat(
                        hash(
                                Path.of(
                                        "src/test/java/com/myfitness/ai/evaluation/legacy/LegacyAiQueryRouter.java")))
                .isEqualTo(manifest.path("legacySourceSha256").asText());
    }

    @Test
    @DisplayName("1000개 변경 전 corpus와 저장 판정은 같은 Router에서 재현되며 hash가 고정된다")
    void reproducesThousandCaseBaseline() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Path dataset = Path.of("src/test/resources/ai-policy/synthetic-baseline-1000-v1.jsonl");
        JsonNode manifest =
                mapper.readTree(
                        Files.readString(
                                Path.of(
                                        "src/test/resources/ai-policy/manifest-synthetic-baseline-1000-v1.json")));
        assertThat(hash(dataset)).isEqualTo(manifest.path("datasetSha256").asText());
        java.util.List<JsonNode> corpus = AiPolicyCorpus.load(dataset, false);
        assertThat(corpus).hasSize(1000);
        assertThat(
                        corpus.stream()
                                .map(item -> item.path("currentQuestion").asText())
                                .distinct()
                                .count())
                .isEqualTo(1000);
        assertThat(corpus.stream().map(item -> item.path("familyId").asText()).distinct().count())
                .isEqualTo(50);
        java.util.Map<String, JsonNode> saved =
                Files.readAllLines(Path.of("docs/testing/ai-policy/baseline-1000-v1/cases.jsonl"))
                        .stream()
                        .map(mapper::readTree)
                        .collect(
                                java.util.stream.Collectors.toMap(
                                        item -> item.path("id").asText(), item -> item));
        LegacyAiQueryRouter router = new LegacyAiQueryRouter();
        int misses = 0;
        int falseBlocks = 0;
        for (JsonNode item : corpus) {
            AiQueryType type = router.route(item.path("currentQuestion").asText(), null, null);
            String action = type == AiQueryType.OUT_OF_SCOPE ? "BLOCK" : "ALLOW";
            assertThat(saved.get(item.path("id").asText()).path("prediction").asText())
                    .isEqualTo(action);
            assertThat(saved.get(item.path("id").asText()).path("predictedTopic").asText())
                    .isEqualTo(type.name());
            if (item.path("goldAction").asText().equals("ALLOW") && action.equals("BLOCK"))
                falseBlocks++;
            if (java.util.List.of("BLOCK", "SAFE_REDIRECT")
                            .contains(item.path("goldAction").asText())
                    && action.equals("ALLOW")) misses++;
        }
        assertThat(misses).isEqualTo(340);
        assertThat(falseBlocks).isEqualTo(40);
    }

    private static String hash(Path path) throws Exception {
        return HexFormat.of()
                .formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
}
