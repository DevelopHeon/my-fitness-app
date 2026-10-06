package com.myfitness.ai.evaluation;

import com.myfitness.ai.application.config.AiPolicyProperties;
import com.myfitness.ai.application.exception.AiPolicyUnavailableException;
import com.myfitness.ai.application.port.out.AiChatGateway.HistoryMessage;
import com.myfitness.ai.application.port.out.AiPolicyGateway.AiPolicyAssessment;
import com.myfitness.ai.application.port.out.AiPolicyGateway.AiPolicyRequest;
import com.myfitness.ai.application.support.policy.AiPolicyDecision;
import com.myfitness.ai.application.support.policy.AiPolicyEvaluator;
import com.myfitness.ai.domain.model.AiMessageRole;
import com.myfitness.ai.infrastructure.client.JevAiPolicyGateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Tag("ai-policy-eval")
class AiPolicyEvaluationTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("고정 corpus를 JEV live/replay로 평가하고 출처·오류·비용을 명시한다")
    void evaluatesCorpus() throws Exception {
        String mode = System.getProperty("aiPolicyEval.mode");
        if (!"jev-live".equals(mode) && !"replay".equals(mode))
            throw new IllegalArgumentException("평가 mode 오류");
        Path dataset =
                Path.of(
                        System.getProperty(
                                "aiPolicyEval.dataset",
                                "src/test/resources/ai-policy/seed-v0.jsonl"));
        boolean reviewed =
                !dataset.getFileName().toString().equals("seed-v0.jsonl")
                        && !Boolean.getBoolean("aiPolicyEval.allowDraft");
        List<JsonNode> corpus = AiPolicyCorpus.load(dataset, reviewed);
        String datasetHash = hash(Files.readAllBytes(dataset));
        String questionsHash =
                hash(mapper.writeValueAsBytes(canonical(JevAiPolicyGateway.questions())));
        AiPolicyProperties properties = new AiPolicyProperties();
        properties.setApiKey(System.getenv("TYPESAFE_API_KEY"));
        properties.setModel(System.getProperty("aiPolicyEval.model", "jev-1.13.0"));
        AiPolicyEvaluator evaluator = new AiPolicyEvaluator(properties);
        if (mode.equals("jev-live")
                && (properties.getApiKey() == null || properties.getApiKey().isBlank())) {
            throw new IllegalStateException("TYPESAFE_API_KEY가 없습니다. live를 대역이나 캐시로 대체하지 않습니다.");
        }
        Map<String, JsonNode> replay =
                mode.equals("replay")
                        ? loadReplay(datasetHash, questionsHash, properties.getModel())
                        : Map.of();
        double price =
                Double.parseDouble(
                        System.getProperty("aiPolicyEval.policyInputUsdPerMillion", "0.042"));
        if (!Double.isFinite(price) || price < 0)
            throw new IllegalArgumentException("입력 token 단가 오류");
        int runs =
                Integer.parseInt(
                        System.getProperty(
                                "aiPolicyEval.runs", mode.equals("jev-live") ? "3" : "1"));
        if (runs < 1 || runs > 3) throw new IllegalArgumentException("평가 runs는 1~3이어야 합니다.");
        List<List<JsonNode>> runRows = new ArrayList<>();
        Path batch = Path.of("build/reports/ai-policy", System.currentTimeMillis() + "-" + mode);
        Files.createDirectories(batch);
        try (JevAiPolicyGateway gateway = new JevAiPolicyGateway(properties, mapper)) {
            for (int run = 1; run <= runs; run++) {
                long warmupTokens = 0;
                boolean warmupKnown = true;
                if (mode.equals("jev-live"))
                    for (int index = 0; index < 10; index++) {
                        try {
                            warmupTokens +=
                                    gateway.assess(request(corpus.get(index % corpus.size())))
                                            .inputTokens();
                        } catch (AiPolicyUnavailableException e) {
                            warmupKnown = false;
                        }
                    }
                List<JsonNode> measured = new ArrayList<>();

                for (JsonNode item : corpus) {
                    Map<String, Object> values = baseValues(item);
                    values.put("datasetHash", datasetHash);
                    values.put("questionsHash", questionsHash);
                    AiPolicyAssessment assessment = null;
                    long started = System.nanoTime();
                    try {
                        if (mode.equals("jev-live")) assessment = gateway.assess(request(item));
                        else {
                            JsonNode cached = replay.get(item.path("id").asText());
                            if (cached == null)
                                throw new IllegalArgumentException("replay case 누락");
                            if (cached.path("assessment").isNull()
                                    || cached.path("assessment").isMissingNode()) {
                                // 과거 평가 코드 문자열은 재해석하지 않고 그대로 보고서에 보존한다.
                                values.put("errorCode", cached.path("errorCode").asText("REPLAY_UNAVAILABLE"));
                                throw new AiPolicyUnavailableException(AiPolicyUnavailableException.Code.POLICY_ERROR);
                            }
                            assessment =
                                    mapper.treeToValue(
                                            cached.path("assessment"), AiPolicyAssessment.class);
                        }
                        AiPolicyDecision decision = evaluator.decide(assessment);
                        values.put("prediction", decision.action().name());
                        values.put("reason", decision.reason());
                        values.put(
                                "predictedTopic",
                                decision.queryType() == null ? null : decision.queryType().name());
                        values.put("assessment", assessment);
                        values.put(
                                "inputTokens",
                                mode.equals("jev-live") ? assessment.inputTokens() : null);
                    } catch (AiPolicyUnavailableException e) {
                        values.put("prediction", "UNAVAILABLE");
                        values.putIfAbsent("errorCode", e.getLogCode());
                    }
                    values.put(
                            "latencyMs",
                            mode.equals("jev-live")
                                    ? TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
                                    : null);
                    measured.add(mapper.valueToTree(values));
                }
                Map<String, Object> metrics =
                        AiPolicyMetrics.calculate(measured, mode.equals("jev-live"), price);
                metrics.put("slices", AiPolicyMetrics.slices(measured));
                runRows.add(measured);
                metrics.put("warmupRequests", mode.equals("jev-live") ? 10 : 0);
                metrics.put(
                        "warmupPolicyCostUsd",
                        mode.equals("jev-live") && warmupKnown
                                ? warmupTokens * price / 1_000_000
                                : null);
                Object measuredCost = metrics.get("policyCostUsd");
                metrics.put(
                        "totalPolicyCostIncludingWarmupUsd",
                        measuredCost instanceof Double cost && warmupKnown
                                ? cost + warmupTokens * price / 1_000_000
                                : null);
                LinkedHashMap<String, Object> manifest = new LinkedHashMap<String, Object>();
                manifest.put("mode", mode);
                manifest.put("scope", "INPUT_GATE_ONLY");
                manifest.put("dataset", dataset.toString());
                manifest.put("datasetHash", datasetHash);
                manifest.put(
                        "labelStatus",
                        corpus.stream()
                                .map(item -> item.path("labelStatus").asText())
                                .distinct()
                                .sorted()
                                .toList());
                manifest.put("caseCount", corpus.size());
                manifest.put("run", run);
                manifest.put("requestedModel", properties.getModel());
                manifest.put(
                        "actualModels",
                        measured.stream()
                                .map(it -> it.path("assessment").path("model").asText())
                                .filter(it -> !it.isBlank())
                                .distinct()
                                .toList());
                manifest.put("policyVersion", properties.getVersion());
                manifest.put("questionsHash", questionsHash);
                manifest.put(
                        "thresholds",
                        Map.of(
                                "review",
                                properties.getReviewThreshold(),
                                "action",
                                properties.getActionThreshold(),
                                "topicConfidence",
                                properties.getTopicConfidence()));
                manifest.put(
                        "policyHash",
                        hash(
                                mapper.writeValueAsBytes(
                                        canonical(
                                                Map.of(
                                                        "questionsHash",
                                                        questionsHash,
                                                        "thresholds",
                                                        manifest.get("thresholds"))))));
                manifest.put("pricePerMillionInputUsd", mode.equals("jev-live") ? price : null);
                manifest.put(
                        "priceStatus",
                        "published-rate-estimate; confirm account billing before promotion");
                manifest.put("concurrency", 1);
                manifest.put("retries", 0);
                manifest.put("timeoutMs", properties.getRequestTimeout().toMillis());
                manifest.put("bootstrapIterations", 2000);
                manifest.put("bootstrapSeed", 20260928);
                manifest.put("generatedAt", Instant.now().toString());
                manifest.put("sourceHash", sourceHash());
                manifest.put(
                        "runtime",
                        Map.of(
                                "java",
                                System.getProperty("java.version"),
                                "os",
                                System.getProperty("os.name")));
                manifest.put("codeSha", git("rev-parse", "HEAD"));
                manifest.put("workingTreeDirty", !git("status", "--porcelain").isBlank());
                manifest.put("promotionReady", false);
                manifest.put(
                        "unmeasured",
                        List.of(
                                "independent holdout quality if draft",
                                "E2E response safety",
                                "generation cost",
                                "whole HTTP latency",
                                "staging availability"));
                Path output = batch.resolve("run-" + run);
                Files.createDirectories(output);
                Files.writeString(
                        output.resolve("manifest.json"),
                        mapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest));
                Files.writeString(
                        output.resolve("metrics.json"),
                        mapper.writerWithDefaultPrettyPrinter().writeValueAsString(metrics));
                Files.writeString(
                        output.resolve("cases.jsonl"),
                        String.join(
                                        "\n",
                                        measured.stream().map(mapper::writeValueAsString).toList())
                                + "\n");
                Files.writeString(output.resolve("comparison.md"), report(mode, manifest, metrics));
                System.out.println("AI policy report: " + output.toAbsolutePath());
            }
        }
        Files.writeString(
                batch.resolve("aggregate.json"),
                mapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(AiPolicyMetrics.aggregateRuns(runRows)));
    }

    private Map<String, Object> baseValues(JsonNode item) {
        LinkedHashMap<String, Object> row = new LinkedHashMap<String, Object>();
        for (String key : List.of("id", "familyId", "goldAction", "goldTopic", "goldHazards"))
            row.put(key, item.get(key));
        LinkedHashSet<String> tags = new LinkedHashSet<String>();
        item.path("sliceTags").forEach(it -> tags.add(it.asText()));
        String text = item.path("currentQuestion").asText();
        tags.add(
                text.codePoints()
                                .anyMatch(
                                        code ->
                                                Character.UnicodeScript.of(code)
                                                        == Character.UnicodeScript.HANGUL)
                        ? "ko"
                        : "non-ko");
        for (String hazard :
                List.of("medical_decision", "unsafe_action", "urgent_signal", "policy_bypass")) {
            if (item.path("goldHazards").path(hazard).asInt() == 1) tags.add(hazard);
        }
        row.put("sliceTags", List.copyOf(tags));
        row.put("split", item.path("split").asText());
        return row;
    }

    private AiPolicyRequest request(JsonNode item) {
        List<HistoryMessage> history = new ArrayList<>();
        for (JsonNode turn : item.path("previousTurns"))
            history.add(
                    new HistoryMessage(
                            AiMessageRole.valueOf(turn.path("role").asText()),
                            turn.path("content").asText()));
        if (history.size() > 4
                || history.stream().mapToInt(it -> it.content().length()).sum() > 2000)
            throw new IllegalArgumentException("평가 이력 한도 초과");
        return new AiPolicyRequest(
                item.path("currentQuestion").asText(),
                item.path("screen").isNull() ? null : item.path("screen").asText(),
                history);
    }

    Map<String, JsonNode> loadReplay(String datasetHash, String questionsHash, String model)
            throws Exception {
        String path = System.getProperty("aiPolicyEval.replay");
        if (path == null || path.isBlank())
            throw new IllegalArgumentException(
                    "replay에는 aiPolicyEval.replay cases.jsonl 경로가 필요합니다.");
        LinkedHashMap<String, JsonNode> result = new LinkedHashMap<String, JsonNode>();
        for (String line : Files.readAllLines(Path.of(path)))
            if (!line.isBlank()) {
                JsonNode row = mapper.readTree(line);
                if (!datasetHash.equals(row.path("datasetHash").asText())
                        || !questionsHash.equals(row.path("questionsHash").asText()))
                    throw new IllegalArgumentException("replay hash 불일치");
                if (!row.path("assessment").isNull()
                        && !row.path("assessment").isMissingNode()
                        && !model.equals(row.path("assessment").path("model").asText()))
                    throw new IllegalArgumentException("replay 모델 불일치");
                if (result.put(row.path("id").asText(), row) != null)
                    throw new IllegalArgumentException("replay ID 중복");
            }
        return result;
    }

    private String report(String mode, Map<String, Object> manifest, Map<String, Object> metrics) {
        return "# AI 질문 정책 평가\n\nmode: "
                + mode
                + " / scope: INPUT_GATE_ONLY / labels: "
                + manifest.get("labelStatus")
                + "\n\n| 지표 | 결과 |\n| --- | --- |\n| 평가 요청 | "
                + metrics.get("requests")
                + " |\n| 제한 누락률 | "
                + metrics.get("restrictedMissRate")
                + " |\n| 정상 오차단률 | "
                + metrics.get("normalFalseBlockRate")
                + " |\n| 동작 macro-F1 | "
                + metrics.get("macroF1")
                + " |\n| 평가 장애 | "
                + metrics.get("unavailable")
                + " |\n| 정책 p95 ms | "
                + metrics.get("policyP95Ms")
                + " |\n| 정책 비용 USD | "
                + metrics.get("policyCostUsd")
                + " |\n\n"
                + "null은 미측정입니다. 초안 seed 결과는 전체 정확도나 승격 근거가 아닙니다. 실제 노출 답변의 안전성·E2E 지연·생성 비용은 이 입력"
                + " gate 평가에 포함되지 않습니다.\n\n"
                + "[Manifest](manifest.json) · [전체 지표·CI](metrics.json) · [케이스](cases.jsonl)\n";
    }

    private static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static Object canonical(Object value) {
        if (value instanceof Map<?, ?> map) {
            TreeMap<String, Object> sorted = new TreeMap<String, Object>();
            map.forEach((key, item) -> sorted.put(key.toString(), canonical(item)));
            return sorted;
        }
        if (value instanceof List<?> list)
            return list.stream().map(AiPolicyEvaluationTest::canonical).toList();
        return value;
    }

    private static String sourceHash() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        List<Path> files = new ArrayList<>(List.of(Path.of("build.gradle.kts")));
        for (String root : List.of("src/main", "src/test"))
            try (Stream<Path> paths = Files.walk(Path.of(root))) {
                files.addAll(paths.filter(Files::isRegularFile).toList());
            }
        files.sort(java.util.Comparator.comparing(Path::toString));
        for (Path file : files) {
            digest.update(file.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(Files.readAllBytes(file));
            digest.update((byte) 0);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String git(String... args) throws Exception {
        List<String> command = new ArrayList<>(List.of("git"));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output =
                new String(
                        process.getInputStream().readAllBytes(),
                        java.nio.charset.StandardCharsets.UTF_8);
        if (process.waitFor() != 0) throw new IllegalStateException("평가 코드 출처 확인 실패");
        return output.trim();
    }
}
