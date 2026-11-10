package br.com.senac.gerenciadordesalas.cadastros;

import br.com.senac.gerenciadordesalas.material.Material;
import br.com.senac.gerenciadordesalas.material.MaterialRepository;
import br.com.senac.gerenciadordesalas.material.MaterialTipo;
import br.com.senac.gerenciadordesalas.professor.Competencia;
import br.com.senac.gerenciadordesalas.professor.CompetenciaRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Validacao docente pelo Responsavel (APR-03, #34) com PostgreSQL real: fila com
 * professor e competencias, validar, rejeitar com motivo, repeticao, conflito na
 * agenda surgido depois da solicitacao e autorizacao.
 */
@Import(RelogioFixoTestConfig.class)
class AprovacaoDocenteIntegrationTest extends IntegracaoPostgresBase {

    private static final String SOLICITANTE = "solicitante.docente@exemplo.test";
    private static final String RESPONSAVEL = "responsavel.docente@exemplo.test";
    private static final OffsetDateTime INICIO = OffsetDateTime.of(2026, 11, 20, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime TERMINO = INICIO.plusHours(2);

    @Autowired UserRepository usuarios;
    @Autowired SalaRepository salas;
    @Autowired ProfessorRepository professores;
    @Autowired CompetenciaRepository competencias;
    @Autowired MaterialRepository materiais;
    @Autowired ReservationService reservationService;

    private Long responsavelId;
    private Long salaId;
    private Long professorId;
    private Long microscopio;

    @BeforeEach
    void preparar() {
        usuarios.save(new User("Ana Docente", SOLICITANTE, "$2a$10$hashBcryptFalso", Role.SOLICITANTE));
        responsavelId = usuarios.save(new User(
                "Rui Docente", RESPONSAVEL, "$2a$10$hashBcryptFalso", Role.RESPONSAVEL)).getId();
        salaId = salas.save(new Sala("Sala Docente", 30, "Bloco P")).getId();
        Competencia quimica = competencias.save(new Competencia("Quimica Docente"));
        Competencia biologia = competencias.save(new Competencia("Biologia Docente"));
        professorId = professores.save(new Professor(
                "Paula Docente", "paula.docente@exemplo.test", Set.of(quimica, biologia))).getId();
        microscopio = materiais.save(new Material(
                "Microscopio", "Microscopio de laboratorio", MaterialTipo.EQUIPAMENTO, null,
                1, "Laboratorio", true)).getId();
        sincronizar();
    }

    @Test
    void fila_mostraProfessorCompetenciasSalaEPeriodo() throws Exception {
        Long id = criarComProfessor(List.of()).id();

        mockMvc.perform(get("/api/aprovacoes").with(user(RESPONSAVEL).roles("RESPONSAVEL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reservaId").value(id))
                .andExpect(jsonPath("$[0].professorNome").value("Paula Docente"))
                .andExpect(jsonPath("$[0].professorCompetencias[0]").value("Biologia Docente"))
                .andExpect(jsonPath("$[0].professorCompetencias[1]").value("Quimica Docente"))
                .andExpect(jsonPath("$[0].salaNome").value("Sala Docente"))
                .andExpect(jsonPath("$[0].inicio").exists())
                .andExpect(jsonPath("$[0].validacaoDocente").value("PENDENTE"));
    }

    @Test
    void responsavel_validaDocente_eADecisaoFicaRegistrada() throws Exception {
        Long id = criarComProfessor(List.of()).id();

        mockMvc.perform(post("/api/reservas/{id}/validar-docente", id)
                        .with(user(RESPONSAVEL).roles("RESPONSAVEL"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADA"))
                .andExpect(jsonPath("$.validacaoDocente").value("APROVADA"));

        assertThat(consultar("SELECT docente_decidido_por_id FROM tb_reservas WHERE id = ?", Long.class, id))
                .isEqualTo(responsavelId);
        assertThat(consultar("SELECT docente_decidido_em IS NOT NULL FROM tb_reservas WHERE id = ?", Boolean.class, id))
                .isTrue();
    }

    @Test
    void responsavel_rejeitaDocenteComMotivo_eOMotivoFicaPreservado() throws Exception {
        Long id = criarComProfessor(List.of()).id();

        mockMvc.perform(post("/api/reservas/{id}/rejeitar-docente", id)
                        .with(user(RESPONSAVEL).roles("RESPONSAVEL"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\": \"  Professora sem a competencia exigida  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJEITADA"))
                .andExpect(jsonPath("$.motivoRejeicao").value("Professora sem a competencia exigida"));

        assertThat(consultar("SELECT docente_motivo_rejeicao FROM tb_reservas WHERE id = ?", String.class, id))
                .isEqualTo("Professora sem a competencia exigida");
    }

    @Test
    void rejeitarDocenteSemMotivo_recebe400() throws Exception {
        Long id = criarComProfessor(List.of()).id();

        mockMvc.perform(post("/api/reservas/{id}/rejeitar-docente", id)
                        .with(user(RESPONSAVEL).roles("RESPONSAVEL"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"));
    }

    @Test
    void validacaoRepetida_recebe409() throws Exception {
        Long id = criarComProfessor(List.of()).id();
        mockMvc.perform(post("/api/reservas/{id}/validar-docente", id)
                        .with(user(RESPONSAVEL).roles("RESPONSAVEL"))
                        .with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/reservas/{id}/validar-docente", id)
                        .with(user(RESPONSAVEL).roles("RESPONSAVEL"))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_DE_ESTADO"));
    }

    @Test
    void conflitoNaAgendaSurgidoDepoisDaSolicitacao_impedeValidacao() throws Exception {
        Long id = criarComProfessor(List.of()).id();
        jdbcTemplate.update("INSERT INTO tb_bloqueios_manutencao (recurso_tipo, recurso_id, motivo, inicio, fim) "
                        + "VALUES ('PROFESSOR', ?, 'Afastamento para capacitacao', ?, ?)",
                professorId, Timestamp.from(INICIO.toInstant()), Timestamp.from(TERMINO.toInstant()));

        mockMvc.perform(post("/api/reservas/{id}/validar-docente", id)
                        .with(user(RESPONSAVEL).roles("RESPONSAVEL"))
                        .with(csrf()))
                .andExpect(status().isConflict());

        assertThat(consultar("SELECT validacao_docente FROM tb_reservas WHERE id = ?", String.class, id))
                .isEqualTo("PENDENTE");
    }

    /**
     * Apontamento do Sourcery no PR #178: o conflito na agenda precisa ser testado sem
     * manutencao, para o teste falhar se a consulta de sobreposicao do professor quebrar.
     * A reserva concorrente e gravada direto no banco (como um dado legado ou importado),
     * em outra sala e sem manutencao: o unico motivo de recusa e a agenda do professor.
     */
    @Test
    void reservaDoProfessorSobrepostaEmOutraSala_impedeValidacao() throws Exception {
        Long id = criarComProfessor(List.of()).id();
        Long outraSala = salas.save(new Sala("Sala Docente 2", 20, "Bloco P")).getId();
        sincronizar();
        Long solicitanteId = consultar("SELECT id FROM tb_usuarios WHERE email = ?", Long.class, SOLICITANTE);
        jdbcTemplate.update("INSERT INTO tb_reservas (solicitante_id, sala_id, professor_id, inicio, termino, "
                        + "finalidade, status) VALUES (?, ?, ?, ?, ?, 'Outra aula da professora', 'APROVADA')",
                solicitanteId, outraSala, professorId,
                Timestamp.from(INICIO.plusMinutes(30).toInstant()),
                Timestamp.from(TERMINO.plusMinutes(30).toInstant()));

        mockMvc.perform(post("/api/reservas/{id}/validar-docente", id)
                        .with(user(RESPONSAVEL).roles("RESPONSAVEL"))
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_DE_ESTADO"));

        assertThat(consultar("SELECT validacao_docente FROM tb_reservas WHERE id = ?", String.class, id))
                .isEqualTo("PENDENTE");
    }

    @Test
    void recursoRestritoEDocente_soFicaAprovadaDepoisDasDuasDecisoes() throws Exception {
        Long id = criarComProfessor(List.of(new ReservationDto.MaterialPedido(microscopio, 1))).id();

        mockMvc.perform(post("/api/reservas/{id}/aprovar", id)
                        .with(user(RESPONSAVEL).roles("RESPONSAVEL"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CRIADA"))
                .andExpect(jsonPath("$.aprovacaoRecurso").value("APROVADA"))
                .andExpect(jsonPath("$.validacaoDocente").value("PENDENTE"));

        mockMvc.perform(post("/api/reservas/{id}/validar-docente", id)
                        .with(user(RESPONSAVEL).roles("RESPONSAVEL"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADA"));
    }

    @ParameterizedTest(name = "{0} recebe 403")
    @ValueSource(strings = {"SOLICITANTE", "ADMINISTRADOR"})
    void outrosPerfis_naoValidamDocente(String perfil) throws Exception {
        Long id = criarComProfessor(List.of()).id();

        String email = SOLICITANTE;
        if ("ADMINISTRADOR".equals(perfil)) {
            email = "admin.negado@exemplo.test";
            usuarios.save(new User("Admin Negado", email, "$2a$10$hashBcryptFalso", Role.ADMINISTRADOR));
        }

        mockMvc.perform(post("/api/reservas/{id}/validar-docente", id)
                        .with(user(email).roles(perfil))
                        .with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/reservas/{id}/rejeitar-docente", id)
                        .with(user(email).roles(perfil))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\": \"Nao posso\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void semAutenticacao_recebe401() throws Exception {
        Long id = criarComProfessor(List.of()).id();

        mockMvc.perform(post("/api/reservas/{id}/validar-docente", id).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    private ReservationDto.Saida criarComProfessor(List<ReservationDto.MaterialPedido> pedidos) {
        return reservationService.criar(new ReservationDto.Entrada(
                salaId, professorId, INICIO, TERMINO, "Aula com professora", pedidos), SOLICITANTE);
    }
}
