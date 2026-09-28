package com.myfitness.ai.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

class AiPolicyReplayTest {
    @TempDir Path directory;

    @Test
    @DisplayName("replay는 데이터·질문 hash와 실제 model이 일치해야 하며 중복 ID를 거절한다")
    void validatesReplayProvenance() throws Exception {
        Path file = directory.resolve("cases.jsonl");
        String old = System.getProperty("aiPolicyEval.replay");
        System.setProperty("aiPolicyEval.replay", file.toString());
        try {
            String row =
                    "{\"id\":\"A01\",\"datasetHash\":\"data\",\"questionsHash\":\"questions\",\"assessment\":{\"model\":\"jev-1.13.0\"}}";
            Files.writeString(file, row);
            AiPolicyEvaluationTest runner = new AiPolicyEvaluationTest();
            assertThat(runner.loadReplay("data", "questions", "jev-1.13.0")).containsKey("A01");
            assertThatThrownBy(() -> runner.loadReplay("other", "questions", "jev-1.13.0"))
                    .hasMessageContaining("hash");
            assertThatThrownBy(() -> runner.loadReplay("data", "questions", "other-model"))
                    .hasMessageContaining("모델");
            Files.writeString(file, row + "\n" + row);
            assertThatThrownBy(() -> runner.loadReplay("data", "questions", "jev-1.13.0"))
                    .hasMessageContaining("중복");
        } finally {
            if (old == null) System.clearProperty("aiPolicyEval.replay");
            else System.setProperty("aiPolicyEval.replay", old);
        }
    }
}
