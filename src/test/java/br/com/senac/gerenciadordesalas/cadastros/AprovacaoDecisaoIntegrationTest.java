package br.com.senac.gerenciadordesalas.cadastros;

import br.com.senac.gerenciadordesalas.material.Material;
import br.com.senac.gerenciadordesalas.material.MaterialRepository;
import br.com.senac.gerenciadordesalas.material.MaterialTipo;
import br.com.senac.gerenciadordesalas.professor.Professor;
import br.com.senac.gerenciadordesalas.professor.ProfessorRepository;
import br.com.senac.gerenciadordesalas.reservas.ReservationDto;
import br.com.senac.gerenciadordesalas.reservas.ReservationService;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Aprovacao e rejeicao de recurso restrito (APR-02, #34) com PostgreSQL real:
 * perfis, estados, motivo preservado, decisao repetida e conflito surgido depois
 * da solicitacao.
 */
@Import(RelogioFixoTestConfig.class)
class AprovacaoDecisaoIntegrationTest extends IntegracaoPostgresBase {

    private static final String SOLICITANTE = "solicitante.decisao@exemplo.test";
    private static final String RESPONSAVEL = "responsavel.decisao@exemplo.test";
    private static final OffsetDateTime INICIO = OffsetDateTime.of(2026, 11, 20, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime TERMINO = INICIO.plusHours(2);

    @Autowired UserRepository usuarios;
    @Autowired SalaRepository salas;
    @Autowired ProfessorRepository professores;
    @Autowired MaterialRepository materiais;
    @Autowired ReservationService reservationService;

    private Long responsavelId;
    private Long salaId;
    private Long professorId;
    private Long microscopio;

    @BeforeEach
    void preparar() {
        usuarios.save(new User("Ana Decisao", SOLICITANTE, "$2a$10$hashBcryptFalso", Role.SOLICITANTE));
        responsavelId = usuarios.save(new User(
                "Rui Responsavel", RESPONSAVEL, "$2a$10$hashBcryptFalso", Role.RESPONSAVEL)).getId();
        salaId = salas.save(new Sala("Sala Decisao", 30, "Bloco D")).getId();
        professorId = professores.save(new Professor(
                "Carla Decisao", "carla.decisao@exemplo.test", Set.of())).getId();
        microscopio = materiais.save(new Material(
                "Microscopio", "Microscopio de laboratorio", MaterialTipo.EQUIPAMENTO, null,
                1, "Laboratorio", true)).getId();
        sincronizar();
    }

    @Test
    void responsavel_aprovaRecursoRestrito_eADecisaoFicaRegistrada() throws Exception {
        Long id = criarRestrita().id();

        mockMvc.perform(post("/api/reservas/{id}/aprovar", id)
                        .with(user(RESPONSAVEL).roles("RESPONSAVEL"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADA"))
                .andExpect(jsonPath("$.aprovacaoRecurso").value("APROVADA"));

        assertThat(consultar("SELECT recurso_decidido_por_id FROM tb_reservas WHERE id = ?", Long.class, id))
                .isEqualTo(responsavelId);
        assertThat(consultar("SELECT recurso_decidido_em IS NOT NULL FROM tb_reservas WHERE id = ?", Boolean.class, id))
                .isTrue();
    }

    @Test
    void responsavel_rejeitaComMotivo_eOMotivoFicaPreservado() throws Exception {
        Long id = criarRestrita().id();

        mockMvc.perform(post("/api/reservas/{id}/rejeitar", id)
                        .with(user(RESPONSAVEL).roles("RESPONSAVEL"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\": \"  Microscopio reservado para pesquisa  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJEITADA"))
                .andExpect(jsonPath("$.motivoRejeicao").value("Microscopio reservado para pesquisa"));

        assertThat(consultar("SELECT recurso_motivo_rejeicao FROM tb_reservas WHERE id = ?", String.class, id))
                .isEqualTo("Microscopio reservado para pesquisa");
    }

    @ParameterizedTest(name = "motivo [{0}] recebe 400")
    @ValueSource(strings = {"{}", "{\"motivo\": \"   \"}"})
    void rejeitarSemMotivo_recebe400(String corpo) throws Exception {
        Long id = criarRestrita().id();

        mockMvc.perform(post("/api/reservas/{id}/rejeitar", id)
                        .with(user(RESPONSAVEL).roles("RESPONSAVEL"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"));

        assertThat(consultar("SELECT status FROM tb_reservas WHERE id = ?", String.class, id))
                .isEqualTo("CRIADA");
    }

    @Test
    void decisaoRepetida_recebe409() throws Exception {
        Long id = criarRestrita().id();
        mockMvc.perform(post("/api/reservas/{id}/aprovar", id)
                        .with(user(RESPONSAVEL).roles("RESPONSAVEL"))
                        .with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/reservas/{id}/aprovar", id)
                        .with(user(RESPONSAVEL).roles("RESPONSAVEL"))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_DE_ESTADO"));
        mockMvc.perform(post("/api/reservas/{id}/rejeitar", id)
                        .with(user(RESPONSAVEL).roles("RESPONSAVEL"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\": \"Tarde demais\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void manutencaoRegistradaDepoisDaSolicitacao_impedeAprovacao() throws Exception {
        Long id = criarRestrita().id();
        jdbcTemplate.update("INSERT INTO tb_bloqueios_manutencao (recurso_tipo, recurso_id, motivo, inicio, fim) "
                        + "VALUES ('SALA', ?, 'Troca do piso', ?, ?)",
                salaId, Timestamp.from(INICIO.toInstant()), Timestamp.from(TERMINO.toInstant()));

        mockMvc.perform(post("/api/reservas/{id}/aprovar", id)
                        .with(user(RESPONSAVEL).roles("RESPONSAVEL"))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_DE_ESTADO"));

        assertThat(consultar("SELECT status FROM tb_reservas WHERE id = ?", String.class, id))
                .isEqualTo("CRIADA");
    }

    @Test
    void reservaSemRecursoRestrito_naoTemAprovacaoDeRecursoParaDecidir() throws Exception {
        ReservationDto.Saida comProfessor = reservationService.criar(new ReservationDto.Entrada(
                salaId, professorId, INICIO, TERMINO, "Aula com professor", List.of()), SOLICITANTE);

        mockMvc.perform(post("/api/reservas/{id}/aprovar", comProfessor.id())
                        .with(user(RESPONSAVEL).roles("RESPONSAVEL"))
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void reservaInexistente_recebe404() throws Exception {
        mockMvc.perform(post("/api/reservas/{id}/aprovar", 999999L)
                        .with(user(RESPONSAVEL).roles("RESPONSAVEL"))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest(name = "{0} recebe 403")
    @ValueSource(strings = {"SOLICITANTE", "ADMINISTRADOR"})
    void outrosPerfis_naoDecidem(String perfil) throws Exception {
        Long id = criarRestrita().id();

        mockMvc.perform(post("/api/reservas/{id}/aprovar", id)
                        .with(user(SOLICITANTE).roles(perfil))
                        .with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/reservas/{id}/rejeitar", id)
                        .with(user(SOLICITANTE).roles(perfil))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\": \"Nao posso\"}"))
                .andExpect(status().isForbidden());

        assertThat(consultar("SELECT status FROM tb_reservas WHERE id = ?", String.class, id))
                .isEqualTo("CRIADA");
    }

    @Test
    void semAutenticacao_recebe401() throws Exception {
        Long id = criarRestrita().id();

        mockMvc.perform(post("/api/reservas/{id}/aprovar", id).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    private ReservationDto.Saida criarRestrita() {
        return reservationService.criar(new ReservationDto.Entrada(
                salaId, null, INICIO, TERMINO, "Aula com microscopio",
                List.of(new ReservationDto.MaterialPedido(microscopio, 1))), SOLICITANTE);
    }
}
