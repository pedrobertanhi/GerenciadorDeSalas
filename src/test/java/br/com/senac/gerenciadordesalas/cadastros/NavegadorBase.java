package br.com.senac.gerenciadordesalas.cadastros;

import br.com.senac.gerenciadordesalas.usuario.Role;
import br.com.senac.gerenciadordesalas.usuario.User;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base dos testes E2E em navegador (Issue #15): aplicação completa em uma porta real, PostgreSQL
 * em container e um Chrome sem interface gráfica fazendo o papel do usuário.
 *
 * <p>Ao contrário da suíte de integração, aqui as requisições do navegador são transações
 * separadas e confirmadas, então os dados não são desfeitos sozinhos. Por isso o banco é limpo antes
 * e depois de cada teste, e a suíte de integração, que confere se as tabelas estão vazias, continua
 * valendo para o PostgreSQL compartilhado.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
abstract class NavegadorBase {

    static final String ADMIN_EMAIL = "admin.e2e@exemplo.test";
    static final String SOLICITANTE_EMAIL = "solicitante.e2e@exemplo.test";
    static final String SENHA = "Senha#E2E2026";

    @DynamicPropertySource
    static void configurarPostgreSql(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> PostgresCompartilhado.POSTGRES.getJdbcUrl());
        registry.add("spring.datasource.username", () -> PostgresCompartilhado.POSTGRES.getUsername());
        registry.add("spring.datasource.password", () -> PostgresCompartilhado.POSTGRES.getPassword());
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @LocalServerPort
    int porta;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    UserRepository usuarios;

    @Autowired
    PasswordEncoder codificador;

    NavegadorE2E navegador;

    @BeforeEach
    void prepararAmbiente() {
        limparBanco();
        usuarios.save(new User("Administrador E2E", ADMIN_EMAIL, codificador.encode(SENHA), Role.ADMINISTRADOR));
        usuarios.save(new User("Solicitante E2E", SOLICITANTE_EMAIL, codificador.encode(SENHA), Role.SOLICITANTE));
        navegador = NavegadorE2E.abrir("http://localhost:" + porta);
    }

    @AfterEach
    void encerrarAmbiente() {
        if (navegador != null) {
            navegador.fechar();
        }
        limparBanco();
    }

    private void limparBanco() {
        jdbcTemplate.execute("TRUNCATE TABLE tb_reservas_materiais, tb_reservas, tb_bloqueios_manutencao, "
                + "tb_professor_competencias, tb_professores, tb_competencias, tb_materiais, salas, tb_usuarios "
                + "RESTART IDENTITY CASCADE");
    }
}