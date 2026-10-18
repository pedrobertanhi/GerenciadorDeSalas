package br.com.senac.gerenciadordesalas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("gerenciador_salas_test")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @DynamicPropertySource
    static void configurarPostgreSql(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void bancoVazioRecebeMigrationInicialEHistoricoDoFlyway() {
        Integer migrationsAplicadas = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                  FROM flyway_schema_history
                 WHERE version = '1'
                   AND success = TRUE
                """,
                Integer.class
        );

        String tabelaCriada = jdbcTemplate.queryForObject(
                "SELECT to_regclass('public.tb_salas')::text",
                String.class
        );

        assertThat(migrationsAplicadas).isEqualTo(1);
        assertThat(tabelaCriada).isEqualTo("tb_salas");
    }
}
