package br.com.senac.gerenciadordesalas.professor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ProfessorRepositoryIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("gerenciador_salas_test")
        .withUsername("postgres").withPassword("postgres");

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired ProfessorRepository professores;
    @Autowired CompetenciaRepository competencias;

    @Test void persisteProfessorEFiltraCompetenciaSemPerderOutras() {
        Competencia java = competencias.saveAndFlush(new Competencia("java"));
        Competencia matematica = competencias.saveAndFlush(new Competencia("matematica"));
        Professor p = professores.saveAndFlush(
            new Professor("Ana", "ana@exemplo.test", Set.of(java, matematica)));

        var filtrados = professores.buscarAtivos("java");
        assertThat(filtrados).hasSize(1);
        assertThat(filtrados.get(0).getId()).isEqualTo(p.getId());
        assertThat(filtrados.get(0).getCompetencias())
            .extracting(Competencia::getNome).containsExactlyInAnyOrder("java", "matematica");
    }

    @Test void desativadoNaoApareceNaListagem() {
        Professor p = professores.saveAndFlush(new Professor("Ana", "ana@exemplo.test", Set.of()));
        p.desativar();
        professores.saveAndFlush(p);
        assertThat(professores.buscarAtivos(null)).isEmpty();
        assertThat(professores.findById(p.getId())).isPresent();
    }

    @Test void bancoRecusaEmailDuplicado() {
        professores.saveAndFlush(new Professor("Ana", "ana@exemplo.test", Set.of()));
        assertThatThrownBy(() -> professores.saveAndFlush(
            new Professor("Outra", "ana@exemplo.test", Set.of())))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}