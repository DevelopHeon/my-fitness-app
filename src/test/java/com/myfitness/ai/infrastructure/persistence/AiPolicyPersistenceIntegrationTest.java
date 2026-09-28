package com.myfitness.ai.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class AiPolicyPersistenceIntegrationTest {
    @Test
    @DisplayName("정책 migration은 기존 요청 row를 유지하고 nullable 정책 컬럼을 추가한다")
    void migratesLegacyRowsWithoutInventingPolicy() {
        DriverManagerDataSource dataSource =
                new DriverManagerDataSource(
                        "jdbc:h2:mem:ai_policy_migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                        "sa",
                        "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("drop all objects");
        // H2 does not recognize PostgreSQL TIMESTAMPTZ shorthand used in the unchanged V6.
        jdbc.execute("create domain TIMESTAMPTZ as timestamp with time zone");
        new ResourceDatabasePopulator(
                        new ClassPathResource("db/migration/V6__create_ai_coach_tables.sql"))
                .execute(dataSource);
        jdbc.update(
                "insert into ai_conversations(user_id,title,created_at,updated_at)"
                    + " values(1,'legacy',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        jdbc.update(
                "insert into ai_messages(conversation_id,role,query_type,content,created_at)"
                    + " values(1,'USER','WORKOUT','운동',CURRENT_TIMESTAMP)");
        jdbc.update(
                "insert into"
                    + " ai_request_logs(user_id,conversation_id,user_message_id,query_type,prompt_version,status,created_at)"
                    + " values(1,1,1,'WORKOUT','v1','SUCCESS',CURRENT_TIMESTAMP)");
        new ResourceDatabasePopulator(
                        new ClassPathResource("db/migration/V10__add_ai_policy_metadata.sql"))
                .execute(dataSource);
        assertThat(jdbc.queryForObject("select count(*) from ai_request_logs", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("select policy_decision from ai_request_logs", String.class))
                .isNull();
        assertThat(jdbc.queryForObject("select status from ai_request_logs", String.class))
                .isEqualTo("SUCCESS");
    }
}
