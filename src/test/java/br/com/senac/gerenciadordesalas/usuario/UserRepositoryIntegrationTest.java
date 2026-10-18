package br.com.senac.gerenciadordesalas.usuario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
class UserRepositoryIntegrationTest {

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
    UserRepository userRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private User novoUsuario(String email, Role perfil) {
        return new User("Usuario Sintetico", email, "$2a$10$hashsinteticodeteste", perfil);
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void usuarioEhPersistidoComCadaPerfilValido(Role perfil) {
        User salvo = userRepository.saveAndFlush(
                novoUsuario(perfil.name().toLowerCase() + "@exemplo.test", perfil));

        String perfilGravado = jdbcTemplate.queryForObject(
                "SELECT perfil FROM tb_usuarios WHERE id = ?", String.class, salvo.getId());

        assertThat(perfilGravado).isEqualTo(perfil.name());
        assertThat(salvo.getCreatedAt()).isNotNull();
        assertThat(salvo.isActive()).isTrue();
    }

    @Test
    void emailDuplicadoEhRecusado() {
        userRepository.saveAndFlush(novoUsuario("ana@exemplo.test", Role.SOLICITANTE));

        assertThatThrownBy(() ->
                userRepository.saveAndFlush(novoUsuario("ana@exemplo.test", Role.RESPONSAVEL)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void emailDuplicadoIgnorandoMaiusculasEhRecusado() {
        userRepository.saveAndFlush(novoUsuario("ana@exemplo.test", Role.SOLICITANTE));

        assertThatThrownBy(() ->
                userRepository.saveAndFlush(novoUsuario("ANA@Exemplo.Test", Role.SOLICITANTE)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void usuarioInativoPermaneceNoHistorico() {
        User usuario = userRepository.saveAndFlush(novoUsuario("inativo@exemplo.test", Role.SOLICITANTE));

        usuario.deactivate();
        userRepository.saveAndFlush(usuario);

        Boolean ativoNoBanco = jdbcTemplate.queryForObject(
                "SELECT ativo FROM tb_usuarios WHERE email = ?", Boolean.class, "inativo@exemplo.test");

        assertThat(ativoNoBanco).isFalse();
        assertThat(userRepository.findByEmailIgnoreCase("INATIVO@exemplo.test")).isPresent();
        assertThat(userRepository.existsByEmailIgnoreCase("inativo@exemplo.test")).isTrue();
    }

    @Test
    void bancoRecusaPerfilInvalido() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO tb_usuarios (nome, email, senha_hash, perfil) VALUES (?, ?, ?, ?)",
                "Perfil Invalido", "perfil.invalido@exemplo.test", "hash", "INVALIDO"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void bancoRecusaEmailComMaiusculas() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO tb_usuarios (nome, email, senha_hash, perfil) VALUES (?, ?, ?, ?)",
                "Email Maiusculo", "MAIUSCULO@EXEMPLO.TEST", "hash", "SOLICITANTE"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}