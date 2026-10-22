package br.com.senac.gerenciadordesalas.material;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class MaterialRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("gerenciador_salas_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    MaterialRepository materiais;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void persisteFiltraEDesativaSemExcluir() {
        Material equipamento = materiais.saveAndFlush(material("Projetor", "PAT-01", 2, true));
        materiais.saveAndFlush(material("Cabo HDMI", null, 10, false));

        assertThat(materiais.buscarAtivos(MaterialTipo.EQUIPAMENTO, true))
                .extracting(Material::getId)
                .containsExactly(equipamento.getId());

        equipamento.desativar();
        materiais.saveAndFlush(equipamento);

        assertThat(materiais.buscarAtivos(MaterialTipo.EQUIPAMENTO, true)).isEmpty();
        assertThat(materiais.findById(equipamento.getId())).isPresent();
    }

    @Test
    void bancoRecusaPatrimonioDuplicado() {
        materiais.saveAndFlush(material("Projetor", "PAT-01", 1, true));

        assertThatThrownBy(() -> materiais.saveAndFlush(material("Notebook", "PAT-01", 1, true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void bancoRecusaQuantidadeNegativa() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO tb_materiais
                    (nome, tipo, quantidade_total, localizacao, restrito, ativo)
                VALUES ('Cabo', 'MATERIAL', -1, 'Almoxarifado', FALSE, TRUE)
                """))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Material material(String nome, String patrimonio, int quantidade, boolean restrito) {
        return new Material(
                nome,
                null,
                patrimonio == null ? MaterialTipo.MATERIAL : MaterialTipo.EQUIPAMENTO,
                patrimonio,
                quantidade,
                "Almoxarifado",
                restrito);
    }
}
