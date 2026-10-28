package br.com.senac.gerenciadordesalas.cadastros;

import br.com.senac.gerenciadordesalas.material.Material;
import br.com.senac.gerenciadordesalas.material.MaterialRepository;
import br.com.senac.gerenciadordesalas.material.MaterialTipo;
import br.com.senac.gerenciadordesalas.reservas.StatusReserva;
import br.com.senac.gerenciadordesalas.salas.Sala;
import br.com.senac.gerenciadordesalas.salas.SalaRepository;
import br.com.senac.gerenciadordesalas.usuario.Role;
import br.com.senac.gerenciadordesalas.usuario.User;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo ponta a ponta de criacao de reserva (RSV-02, #24): happy path,
 * entradas invalidas e autorizacao por perfil, com PostgreSQL real via
 * Testcontainers.
 */
class ReservationCriacaoIntegrationTest extends IntegracaoPostgresBase {

    @Autowired UserRepository usuarios;
    @Autowired SalaRepository salas;
    @Autowired MaterialRepository materiais;

    private static final String EMAIL = "solicitante@exemplo.test";
    private Long salaId;
    private Long materialId;
    private final OffsetDateTime inicio = OffsetDateTime.of(
            2026, 11, 10, 9, 0, 0, 0, ZoneOffset.UTC);
    private final OffsetDateTime termino = inicio.plusHours(2);

    @BeforeEach
    void preparar() {
        usuarios.save(new User(
                "Ana Souza", EMAIL, "$2a$10$hashBcryptFalso", Role.SOLICITANTE));
        Sala sala = salas.save(new Sala("Sala 101", 30, "Bloco A - 1o andar"));
        Material projetor = materiais.save(new Material(
                "Projetor", "Projetor HDMI", MaterialTipo.EQUIPAMENTO, "P-0001",
                5, "Almoxarifado", false));
        sincronizar();
        this.salaId = sala.getId();
        this.materialId = projetor.getId();
    }

    private String corpoValido() {
        return """
                {
                  "salaId": %d,
                  "inicio": "%s",
                  "termino": "%s",
                  "finalidade": "Aula de POO",
                  "materiais": [
                    {"materialId": %d, "quantidade": 2}
                  ]
                }
                """.formatted(salaId, inicio, termino, materialId);
    }

    @Test
    void solicitante_criaReservaValida_recebe201ComLocationESaida() throws Exception {
        mockMvc.perform(post("/api/reservas")
                        .with(user(EMAIL).roles("SOLICITANTE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoValido()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.salaId").value(salaId))
                .andExpect(jsonPath("$.finalidade").value("Aula de POO"))
                .andExpect(jsonPath("$.status").value(StatusReserva.CRIADA.name()))
                .andExpect(jsonPath("$.materiais[0].quantidade").value(2));

        assertThat(contar("tb_reservas")).isEqualTo(1);
        assertThat(contar("tb_reservas_materiais")).isEqualTo(1);
    }

    @Test
    void semAutenticacao_recebe401() throws Exception {
        mockMvc.perform(post("/api/reservas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoValido()))
                .andExpect(status().isUnauthorized());

        assertThat(contar("tb_reservas")).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"RESPONSAVEL", "ADMINISTRADOR"})
    void perfilSemPermissaoDeCriacao_recebe403(String perfil) throws Exception {
        mockMvc.perform(post("/api/reservas")
                        .with(user(EMAIL).roles(perfil))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoValido()))
                .andExpect(status().isForbidden());

        assertThat(contar("tb_reservas")).isZero();
    }

    @Test
    void corpoSemFinalidade_recebe400() throws Exception {
        String corpo = """
                {
                  "salaId": %d,
                  "inicio": "%s",
                  "termino": "%s",
                  "materiais": []
                }
                """.formatted(salaId, inicio, termino);
        mockMvc.perform(post("/api/reservas")
                        .with(user(EMAIL).roles("SOLICITANTE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"));

        assertThat(contar("tb_reservas")).isZero();
    }

    @Test
    void salaInexistente_recebe404() throws Exception {
        String corpo = """
                {
                  "salaId": 999999,
                  "inicio": "%s",
                  "termino": "%s",
                  "finalidade": "Aula fantasma",
                  "materiais": []
                }
                """.formatted(inicio, termino);
        mockMvc.perform(post("/api/reservas")
                        .with(user(EMAIL).roles("SOLICITANTE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));

        assertThat(contar("tb_reservas")).isZero();
    }

    @Test
    void quantidadeAcimaDoSaldo_recebe409() throws Exception {
        String corpo = """
                {
                  "salaId": %d,
                  "inicio": "%s",
                  "termino": "%s",
                  "finalidade": "Aula com excesso",
                  "materiais": [
                    {"materialId": %d, "quantidade": 999}
                  ]
                }
                """.formatted(salaId, inicio, termino, materialId);
        mockMvc.perform(post("/api/reservas")
                        .with(user(EMAIL).roles("SOLICITANTE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_DE_ESTADO"));

        assertThat(contar("tb_reservas")).isZero();
    }
}
