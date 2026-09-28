package com.myfitness.ai.evaluation;

import com.myfitness.ai.application.port.out.AiPolicyGateway;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class AiPolicyCorpus {
    static List<JsonNode> load(Path path, boolean requireReviewed) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<JsonNode> cases =
                Files.readAllLines(path).stream()
                        .filter(line -> !line.isBlank())
                        .map(mapper::readTree)
                        .toList();
        validate(cases, requireReviewed);
        return cases;
    }

    static void validate(List<JsonNode> cases, boolean requireReviewed) {
        if (cases.isEmpty()) throw new IllegalArgumentException("평가 데이터가 비어 있습니다.");
        HashSet<String> ids = new HashSet<String>();
        HashMap<String, String> familySplits = new HashMap<String, String>();
        for (JsonNode item : cases) {
            for (String field :
                    List.of(
                            "id",
                            "familyId",
                            "split",
                            "currentQuestion",
                            "goldAction",
                            "labelStatus",
                            "labelRationale")) {
                if (!item.path(field).isString() || item.path(field).asText().isBlank())
                    throw new IllegalArgumentException("누락 필드: " + field);
            }
            if (!ids.add(item.path("id").asText())) throw new IllegalArgumentException("중복 ID");
            String family = item.path("familyId").asText();
            String split = item.path("split").asText();

            String previous = familySplits.putIfAbsent(family, split);
            if (previous != null && !previous.equals(split))
                throw new IllegalArgumentException("family split 누수");
            if (!Set.of("ALLOW", "BLOCK", "SAFE_REDIRECT", "CLARIFY")
                    .contains(item.path("goldAction").asText()))
                throw new IllegalArgumentException("정답 동작 오류");
            if (!item.path("previousTurns").isArray()
                    || !item.path("goldReasons").isArray()
                    || item.path("goldReasons").isEmpty())
                throw new IllegalArgumentException("이력/라벨 근거 누락");
            if (item.path("goldAction").asText().equals("ALLOW")
                    && !AiPolicyGateway.TOPICS.contains(item.path("goldTopic").asText()))
                throw new IllegalArgumentException("정답 주제 누락");
            for (String hazard :
                    List.of(
                            "medical_decision",
                            "unsafe_action",
                            "urgent_signal",
                            "policy_bypass")) {
                JsonNode gold = item.path("goldHazards").path(hazard);
                if (!gold.isIntegralNumber() || (gold.asInt() != 0 && gold.asInt() != 1))
                    throw new IllegalArgumentException("위험 정답 누락");
            }
            if (requireReviewed
                    && (!item.path("labelStatus").asText().equals("reviewed")
                            || !item.path("reviewedBy").isArray()
                            || item.path("reviewedBy").size() < 2
                            || item.path("reviewedBy")
                                    .get(0)
                                    .asText()
                                    .equals(item.path("reviewedBy").get(1).asText()))) {
                throw new IllegalArgumentException("독립 검토된 정답이 필요합니다.");
            }
        }
    }
}
