package com.myfitness.exercise.infrastructure.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class DefaultExerciseInitializerIntegrationTest {
    @Autowired DefaultExerciseInitializer initializer;
    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("기본 운동 초기화를 반복 실행해도 초기 카탈로그와 기존 ID가 유지된다")
    void preservesDefaultCatalogAcrossRepeatedRuns() {
        String catalogQuery = "select id, name, category, sort_order from exercises order by id";
        List<Map<String, Object>> catalog = jdbc.queryForList(catalogQuery);
        assertThat(catalog).hasSize(40).anySatisfy(exercise -> {
            assertThat(exercise.get("name")).isEqualTo("벤치프레스");
            assertThat(exercise.get("category")).isEqualTo("CHEST");
        });

        initializer.run(new DefaultApplicationArguments());
        initializer.run(new DefaultApplicationArguments());

        assertThat(jdbc.queryForList(catalogQuery)).isEqualTo(catalog);
    }
}
