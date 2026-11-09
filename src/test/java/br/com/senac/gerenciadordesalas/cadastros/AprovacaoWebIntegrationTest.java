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
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tela de aprovacoes (UI-11, #34) com PostgreSQL real: fila, tela de decisao, aprovar,
 * rejeitar com motivo, validar docente, erros de decisao repetida e de conflito, e
 * acesso exclusivo do Responsavel.
 */
@Import(RelogioFixoTestConfig.class)
class AprovacaoWebIntegrationTest extends IntegracaoPostgresBase {

    private static final String SOLICITANTE = "solicitante.tela@exemplo.test";
    private static final String RESPONSAVEL = "responsavel.tela@exemplo.test";
    private static final OffsetDateTime INICIO = OffsetDateTime.of(2026, 11, 20, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime TERMINO = INICIO.plusHours(2);

    @Autowired UserRepository usuarios;
    @Autowired SalaRepository salas;
    @Autowired ProfessorRepository professores;
    @Autowired CompetenciaRepository competencias;
    @Autowired MaterialRepository materiais;
    @Autowired ReservationService reservationService;

    private Long salaA;
    private Long salaB;
    private Long professorId;
    private Long microscopio;

    @BeforeEach
    void preparar() {
        usuarios.save(new User("Ana Tela", SOLICITANTE, "$2a$10$hashBcryptFalso", Role.SOLICITANTE));
        usuarios.save(new User("Rui Tela", RESPONSAVEL, "$2a$10$hashBcryptFalso", Role.RESPONSAVEL));
        salaA = salas.save(new Sala("Sala Tela A", 30, "Bloco T")).getId();
        salaB = salas.save(new Sala("Sala Tela B", 30, "Bloco T")).getId();
        Competencia quimica = competencias.save(new Competencia("Quimica Tela"));
        professorId = professores.save(new Professor(
                "Paula Tela", "paula.tela@exemplo.test", Set.of(quimica))).getId();
        microscopio = materiais.save(new Material(
                "Microscopio", "Microscopio de laboratorio", MaterialTipo.EQUIPAMENTO, null,
                2, "Laboratorio", true)).getId();
        sincronizar();
    }

    @Test
    void fila_mostraPendenciasDaSolicitacaoMaisAntigaParaAMaisRecente() throws Exception {
        criarRestrita(salaA);
        criarComProfessor(salaB);

        String html = mockMvc.perform(get("/aprovacoes").with(user(RESPONSAVEL).roles("RESPONSAVEL")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Aprovações pendentes", "Ana Tela", "Recurso restrito", "Validação docente");
        assertThat(html.indexOf("Sala Tela A")).isLessThan(html.indexOf("Sala Tela B"));
    }

    @Test
    void telaDeDecisao_mostraDadosNecessariosParaDecidir() throws Exception {
        Long id = criarComProfessorEMicroscopio(salaA);

        mockMvc.perform(get("/aprovacoes/{id}", id).with(user(RESPONSAVEL).roles("RESPONSAVEL")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Paula Tela")))
                .andExpect(content().string(containsString("Quimica Tela")))
                .andExpect(content().string(containsString("Microscopio (1), restrito")))
                .andExpect(content().string(containsString("Aprovar recurso")))
                .andExpect(content().string(containsString("Validar docente")));
    }

    @Test
    void aprovarPelaTela_aprovaEVoltaParaAFilaComMensagem() throws Exception {
        Long id = criarRestrita(salaA);

        mockMvc.perform(post("/aprovacoes/{id}/aprovar", id)
                        .with(user(RESPONSAVEL).roles("RESPONSAVEL"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/aprovacoes"))
                .andExpect(flash().attribute("sucesso", "Recurso restrito aprovado."));

        assertThat(statusDaReserva(id)).isEqualTo("APROVADA");
    }

    @Test
    void rejeitarPelaTela_semMotivo_voltaParaADecisaoComErro() throws Exception {
        Long id = criarRestrita(salaA);

        mockMvc.perform(post("/aprovacoes/{id}/rejeitar", id)
                        .with(user(RESPONSAVEL).roles("RESPONSAVEL"))
                        .with(csrf())
                        .param("motivo", "   "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/aprovacoes/" + id))
                .andExpect(flash().attribute("erro", "Informe o motivo da rejeição."));

        assertThat(statusDaReserva(id)).isEqualTo("CRIADA");
    }

    @Test
    void rejeitarPelaTela_comMotivo_rejeitaEPreservaOMotivo() throws Exception {
        Long id = criarRestrita(salaA);

        mockMvc.perform(post("/aprovacoes/{id}/rejeitar", id)
                        .with(user(RESPONSAVEL).roles("RESPONSAVEL"))
                        .with(csrf())
                        .param("motivo", "Microscopio reservado para pesquisa"))
                .andExpect(redirectedUrl("/aprovacoes"))
                .andExpect(flash().attribute("sucesso", "Reserva rejeitada e motivo registrado."));

        assertThat(statusDaReserva(id)).isEqualTo("REJEITADA");
        assertThat(consultar("SELECT recurso_motivo_rejeicao FROM tb_reservas WHERE id = ?", String.class, id))
                .isEqualTo("Microscopio reservado para pesquisa");
    }

    @Test
    void validarEDepoisRejeitarDocentePelaTela() throws Exception {
        Long validada = criarComProfessor(salaA);
        // A mesma professora nao pode estar em dois lugares ao mesmo tempo: segunda aula mais tarde.
        Long rejeitada = criar(salaB, professorId, List.of(), INICIO.plusHours(3), TERMINO.plusHours(3));

        mockMvc.perform(post("/aprovacoes/{id}/validar-docente", validada)
                        .with(user(RESPONSAVEL).roles("RESPONSAVEL"))
                        .with(csrf()))
                .andExpect(flash().attribute("sucesso", "Alocação docente validada."));
        mockMvc.perform(post("/aprovacoes/{id}/rejeitar-docente", rejeitada)
                        .with(user(RESPONSAVEL).roles("RESPONSAVEL"))
                        .with(csrf())
                        .param("motivo", "Agenda da professora mudou"))
                .andExpect(flash().attribute("sucesso", "Alocação docente rejeitada e motivo registrado."));

        assertThat(statusDaReserva(validada)).isEqualTo("APROVADA");
        assertThat(statusDaReserva(rejeitada)).isEqualTo("REJEITADA");
    }

    @Test
    void decisaoRepetidaPelaTela_mostraErroSemMudarAReserva() throws Exception {
        Long id = criarRestrita(salaA);
        mockMvc.perform(post("/aprovacoes/{id}/aprovar", id)
                .with(user(RESPONSAVEL).roles("RESPONSAVEL")).with(csrf()));

        mockMvc.perform(post("/aprovacoes/{id}/rejeitar", id)
                        .with(user(RESPONSAVEL).roles("RESPONSAVEL"))
                        .with(csrf())
                        .param("motivo", "Tarde demais"))
                .andExpect(redirectedUrl("/aprovacoes"))
                .andExpect(flash().attribute("erro", containsString("nao esta pendente")));

        assertThat(statusDaReserva(id)).isEqualTo("APROVADA");
    }

    @Test
    void conflitoAtualizado_impedeAprovacaoPelaTela() throws Exception {
        Long id = criarRestrita(salaA);
        jdbcTemplate.update("INSERT INTO tb_bloqueios_manutencao (recurso_tipo, recurso_id, motivo, inicio, fim) "
                        + "VALUES ('SALA', ?, 'Troca do piso', ?, ?)",
                salaA, Timestamp.from(INICIO.toInstant()), Timestamp.from(TERMINO.toInstant()));

        mockMvc.perform(post("/aprovacoes/{id}/aprovar", id)
                        .with(user(RESPONSAVEL).roles("RESPONSAVEL"))
                        .with(csrf()))
                .andExpect(redirectedUrl("/aprovacoes"))
                .andExpect(flash().attribute("erro", containsString("manutencao")));

        assertThat(statusDaReserva(id)).isEqualTo("CRIADA");
    }

    @Test
    void reservaQueNaoEstaMaisPendente_voltaParaAFilaComAviso() throws Exception {
        Long id = criarRestrita(salaA);
        mockMvc.perform(post("/aprovacoes/{id}/aprovar", id)
                .with(user(RESPONSAVEL).roles("RESPONSAVEL")).with(csrf()));

        mockMvc.perform(get("/aprovacoes/{id}", id).with(user(RESPONSAVEL).roles("RESPONSAVEL")))
                .andExpect(redirectedUrl("/aprovacoes"))
                .andExpect(flash().attribute("erro", "Esta reserva não está mais aguardando decisão."));
    }

    @ParameterizedTest(name = "{0} recebe 403 na tela de aprovacoes")
    @ValueSource(strings = {"SOLICITANTE", "ADMINISTRADOR"})
    void outrosPerfis_naoAcessamNemDecidem(String perfil) throws Exception {
        Long id = criarRestrita(salaA);

        mockMvc.perform(get("/aprovacoes").with(user(SOLICITANTE).roles(perfil)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/aprovacoes/{id}/aprovar", id).with(user(SOLICITANTE).roles(perfil)).with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(statusDaReserva(id)).isEqualTo("CRIADA");
    }

    @Test
    void visitante_eLevadoParaOLogin() throws Exception {
        // A seguranca so redireciona para o login quem pede HTML (navegador); API recebe 401.
        mockMvc.perform(get("/aprovacoes").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    private String statusDaReserva(Long id) {
        return consultar("SELECT status FROM tb_reservas WHERE id = ?", String.class, id);
    }

    private Long criarRestrita(Long salaId) {
        return criar(salaId, null, List.of(new ReservationDto.MaterialPedido(microscopio, 1)));
    }

    private Long criarComProfessor(Long salaId) {
        return criar(salaId, professorId, List.of());
    }

    private Long criarComProfessorEMicroscopio(Long salaId) {
        return criar(salaId, professorId, List.of(new ReservationDto.MaterialPedido(microscopio, 1)));
    }

    private Long criar(Long salaId, Long professor, List<ReservationDto.MaterialPedido> pedidos) {
        return criar(salaId, professor, pedidos, INICIO, TERMINO);
    }

    private Long criar(Long salaId, Long professor, List<ReservationDto.MaterialPedido> pedidos,
                       OffsetDateTime inicio, OffsetDateTime termino) {
        return reservationService.criar(new ReservationDto.Entrada(
                salaId, professor, inicio, termino, "Aula da tela", pedidos), SOLICITANTE).id();
    }
}
