package br.com.senac.gerenciadordesalas.cadastros;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base da suíte de integração dos cadastros (RES-04): Spring Boot completo, MockMvc com
 * segurança ligada e PostgreSQL real em container.
 *
 * Isolamento: cada teste roda dentro de uma transação que é desfeita ao final, e antes
 * de cada teste conferimos que as tabelas estão vazias. Se algum teste deixasse dado
 * para trás, o seguinte falharia na hora, com o nome da tabela contaminada.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@Transactional
abstract class IntegracaoPostgresBase {

    private static final String[] TABELAS = {
            "tb_professor_competencias", "tb_professores", "tb_competencias", "salas", "tb_materiais"
    };

    @DynamicPropertySource
    static void configurarPostgreSql(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> PostgresCompartilhado.POSTGRES.getJdbcUrl());
        registry.add("spring.datasource.username", () -> PostgresCompartilhado.POSTGRES.getUsername());
        registry.add("spring.datasource.password", () -> PostgresCompartilhado.POSTGRES.getPassword());
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @PersistenceContext
    protected EntityManager entityManager;

    @BeforeEach
    void bancoDeveEstarVazioNoInicioDeCadaTeste() {
        for (String tabela : TABELAS) {
            assertThat(contar(tabela))
                    .as("a tabela %s foi contaminada por um teste anterior", tabela)
                    .isZero();
        }
    }

    /** Envia ao banco o que o Hibernate ainda guarda em memória, para o JDBC enxergar. */
    protected void sincronizar() {
        entityManager.flush();
    }

    protected int contar(String tabela) {
        sincronizar();
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tabela, Integer.class);
        return total == null ? 0 : total;
    }

    protected <T> T consultar(String sql, Class<T> tipo, Object... argumentos) {
        sincronizar();
        return jdbcTemplate.queryForObject(sql, tipo, argumentos);
    }
}