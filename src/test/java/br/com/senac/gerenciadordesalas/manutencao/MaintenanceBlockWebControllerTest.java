package br.com.senac.gerenciadordesalas.manutencao;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import br.com.senac.gerenciadordesalas.erro.CorrelationIdFilter;
import br.com.senac.gerenciadordesalas.erro.RecursoNaoEncontradoException;
import br.com.senac.gerenciadordesalas.material.MaterialDto;
import br.com.senac.gerenciadordesalas.material.MaterialService;
import br.com.senac.gerenciadordesalas.material.MaterialTipo;
import br.com.senac.gerenciadordesalas.professor.ProfessorDto;
import br.com.senac.gerenciadordesalas.professor.ProfessorService;
import br.com.senac.gerenciadordesalas.reservas.ReservationService;
import br.com.senac.gerenciadordesalas.salas.Sala;
import br.com.senac.gerenciadordesalas.salas.SalaService;
import br.com.senac.gerenciadordesalas.seguranca.RespostasDeSeguranca;
import br.com.senac.gerenciadordesalas.seguranca.SecurityConfig;
import br.com.senac.gerenciadordesalas.seguranca.UsuarioDetailsService;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Telas de manutenção (UI-08, #15) renderizadas de verdade, com a segurança real: agenda e histórico,
 * registro, edição, conclusão e cancelamento, validação de período e motivo, impacto sobre a
 * disponibilidade, autorização por perfil e marcação responsiva.
 */
@WebMvcTest(MaintenanceBlockWebController.class)
@Import({SecurityConfig.class, RespostasDeSeguranca.class, UsuarioDetailsService.class, CorrelationIdFilter.class})
@ActiveProfiles("test")
class MaintenanceBlockWebControllerTest {

    private static final String VIEWPORT = "width=device-width, initial-scale=1";
    private static final String INICIO = "2099-01-10T08:00";
    private static final String FIM = "2099-01-10T18:00";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    MaintenanceBlockService service;

    @MockitoBean
    SalaService salas;

    @MockitoBean
    ProfessorService professores;

    @MockitoBean
    MaterialService materiais;

    @MockitoBean
    ReservationService reservas;

    @MockitoBean
    UserRepository userRepository;

    private static RequestPostProcessor admin() {
        return user("admin").roles("ADMINISTRADOR");
    }

    private static RequestPostProcessor solicitante() {
        return user("solicitante").roles("SOLICITANTE");
    }

    @BeforeEach
    void recursosCadastrados() {
        Sala sala = new Sala("Sala 101", 30, "Bloco A");
        sala.setId(12L);
        Sala inativa = new Sala("Sala Antiga", 10, "Bloco Z");
        inativa.setId(13L);
        inativa.setAtiva(false);
        when(salas.listarTodas()).thenReturn(List.of(sala, inativa));
        when(professores.listar(null)).thenReturn(
                List.of(new ProfessorDto.Saida(7L, "Ana Souza", "ana@exemplo.test", true, List.of())));
        when(materiais.listar(null, null)).thenReturn(List.of(new MaterialDto.Saida(
                3L, "Projetor", "Projetor HDMI", MaterialTipo.EQUIPAMENTO, "PAT-1", 1, "Almoxarifado", false, true)));
    }

    private static BloqueioDto.Saida bloqueio(long id, long recursoId, StatusBloqueio status,
                                              String inicio, String fim, String encerradoEm, String motivo) {
        return new BloqueioDto.Saida(id, TipoRecurso.SALA, recursoId, motivo,
                OffsetDateTime.parse(inicio), OffsetDateTime.parse(fim), status,
                encerradoEm == null ? null : OffsetDateTime.parse(encerradoEm));
    }

    private static BloqueioDto.Saida futuro(long id) {
        return bloqueio(id, 12L, StatusBloqueio.ATIVO, "2099-01-10T08:00:00-03:00",
                "2099-01-10T18:00:00-03:00", null, "Troca do projetor");
    }

    private static BloqueioDto.Saida emAndamento(long id) {
        return bloqueio(id, 12L, StatusBloqueio.ATIVO, "2020-01-01T08:00:00-03:00",
                "2099-12-31T18:00:00-03:00", null, "Troca do projetor");
    }

    private static BloqueioDto.Saida concluido(long id) {
        return bloqueio(id, 12L, StatusBloqueio.CONCLUIDO, "2025-06-01T08:00:00-03:00",
                "2025-06-01T18:00:00-03:00", "2025-06-01T12:00:00-03:00", "Troca do projetor");
    }

    private static BloqueioDto.Saida cancelado(long id) {
        return bloqueio(id, 12L, StatusBloqueio.CANCELADO, "2099-01-10T08:00:00-03:00",
                "2099-01-10T18:00:00-03:00", "2026-11-01T10:00:00-03:00", "Troca do projetor");
    }

    // ---------- agenda e histórico ----------

    @ParameterizedTest
    @ValueSource(strings = {"SOLICITANTE", "RESPONSAVEL", "ADMINISTRADOR"})
    void todosOsPerfisConsultamAAgenda(String perfil) throws Exception {
        when(service.listar(null, null, null)).thenReturn(List.of(futuro(5)));

        mvc.perform(get("/manutencoes").with(user("leitor").roles(perfil)))
                .andExpect(status().isOk())
                .andExpect(view().name("manutencoes/lista"))
                .andExpect(content().string(containsString("Sala · Sala 101")))
                .andExpect(content().string(containsString("10/01/2099 08:00 a 10/01/2099 18:00")))
                .andExpect(content().string(containsString("Troca do projetor")))
                .andExpect(content().string(containsString("Ativo")));
    }

    @Test
    void administradorVeRegistrarEEditarEQuemNaoEAdministradorNao() throws Exception {
        when(service.listar(null, null, null)).thenReturn(List.of(futuro(5)));

        mvc.perform(get("/manutencoes").with(admin()))
                .andExpect(content().string(containsString("Registrar bloqueio")))
                .andExpect(content().string(containsString("/manutencoes/5/editar")));
        mvc.perform(get("/manutencoes").with(solicitante()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Registrar bloqueio"))))
                .andExpect(content().string(not(containsString("/editar"))));
    }

    @Test
    void concluidosECanceladosContinuamNaListaComoHistoricoSemEdicao() throws Exception {
        when(service.listar(null, null, null)).thenReturn(List.of(concluido(6), cancelado(7)));

        mvc.perform(get("/manutencoes").with(admin()))
                .andExpect(content().string(containsString("Concluído")))
                .andExpect(content().string(containsString("Cancelado")))
                .andExpect(content().string(containsString("/manutencoes/6")))
                .andExpect(content().string(containsString("/manutencoes/7")))
                .andExpect(content().string(not(containsString("/manutencoes/6/editar"))))
                .andExpect(content().string(not(containsString("/manutencoes/7/editar"))));
    }

    @Test
    void visitanteSemLoginVaiParaALogin() throws Exception {
        mvc.perform(get("/manutencoes").header("Accept", "text/html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(service);
    }

    @Test
    void agendaSemBloqueiosMostraMensagemVazia() throws Exception {
        when(service.listar(null, null, null)).thenReturn(List.of());

        mvc.perform(get("/manutencoes").with(solicitante()))
                .andExpect(content().string(containsString("Nenhum bloqueio encontrado.")));
    }

    @Test
    void recursoSemCadastroMostraSoOIdentificador() throws Exception {
        when(service.listar(null, null, null)).thenReturn(List.of(
                bloqueio(5, 99L, StatusBloqueio.ATIVO, "2099-01-10T08:00:00-03:00",
                        "2099-01-10T18:00:00-03:00", null, "Troca")));

        mvc.perform(get("/manutencoes").with(solicitante()))
                .andExpect(content().string(containsString("Sala · #99")));
    }

    @Test
    void filtrosSaoRepassadosAoServico() throws Exception {
        when(service.listar(TipoRecurso.SALA, null, StatusBloqueio.ATIVO)).thenReturn(List.of(futuro(5)));

        mvc.perform(get("/manutencoes").param("tipo", "SALA").param("status", "ATIVO").with(solicitante()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Troca do projetor")));

        verify(service).listar(TipoRecurso.SALA, null, StatusBloqueio.ATIVO);
    }

    @Test
    void filtroInvalidoMostraMensagemEEIgnorado() throws Exception {
        when(service.listar(null, null, null)).thenReturn(List.of());

        mvc.perform(get("/manutencoes").param("tipo", "FERRAMENTA").with(solicitante()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("não é válido")));

        verify(service).listar(null, null, null);
    }

    @Test
    void paginacaoMostraDezPorPaginaEOsLinksDeNavegacao() throws Exception {
        List<BloqueioDto.Saida> vinteECinco = IntStream.rangeClosed(1, 25).mapToObj(i -> futuro(i)).toList();
        when(service.listar(null, null, null)).thenReturn(vinteECinco);

        mvc.perform(get("/manutencoes").with(solicitante()))
                .andExpect(content().string(containsString("Página 1 de 3")))
                .andExpect(content().string(containsString("page=1")))
                .andExpect(content().string(containsString("/manutencoes/10")))
                .andExpect(content().string(not(containsString("/manutencoes/11\""))));
        mvc.perform(get("/manutencoes").param("page", "2").with(solicitante()))
                .andExpect(content().string(containsString("Página 3 de 3")))
                .andExpect(content().string(containsString("/manutencoes/25")));
    }

    @Test
    void avisoDeNaoEncontradoSoAceitaOValorConhecido() throws Exception {
        when(service.listar(null, null, null)).thenReturn(List.of());

        mvc.perform(get("/manutencoes").param("aviso", "nao-encontrado").with(solicitante()))
                .andExpect(content().string(containsString("Bloqueio não encontrado.")));
        mvc.perform(get("/manutencoes").param("aviso", "<script>alert(1)</script>").with(solicitante()))
                .andExpect(content().string(not(containsString("alert(1)"))));
    }

    @Test
    void motivoComHtmlEExibidoComEscape() throws Exception {
        when(service.listar(null, null, null)).thenReturn(List.of(
                bloqueio(5, 12L, StatusBloqueio.ATIVO, "2099-01-10T08:00:00-03:00",
                        "2099-01-10T18:00:00-03:00", null, "<script>alert(1)</script>")));

        mvc.perform(get("/manutencoes").with(solicitante()))
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
                .andExpect(content().string(containsString("&lt;script&gt;")));
    }

    // ---------- detalhe e impacto ----------

    @Test
    void detalheDeBloqueioFuturoMostraImpactoEAcoesSemConcluir() throws Exception {
        when(service.consultar(5L)).thenReturn(futuro(5));

        mvc.perform(get("/manutencoes/5").with(admin()))
                .andExpect(status().isOk())
                .andExpect(view().name("manutencoes/detalhe"))
                .andExpect(content().string(containsString("Impacto sobre a disponibilidade")))
                .andExpect(content().string(containsString("consta como indisponível por manutenção")))
                .andExpect(content().string(containsString("Nenhuma reserva ativa neste período.")))
                .andExpect(content().string(containsString("Editar")))
                .andExpect(content().string(containsString("Cancelar bloqueio")))
                .andExpect(content().string(not(containsString("Concluir manutenção"))));
    }

    @Test
    void detalheDeBloqueioEmAndamentoOferecePossibilidadeDeConcluir() throws Exception {
        when(service.consultar(5L)).thenReturn(emAndamento(5));

        mvc.perform(get("/manutencoes/5").with(admin()))
                .andExpect(content().string(containsString("Concluir manutenção")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void impactoMostraAsReservasAtivasSemCancelarNenhuma() throws Exception {
        when(service.consultar(5L)).thenReturn(futuro(5));
        when(reservas.contarReservasAtivasNoPeriodo(eq(TipoRecurso.SALA), eq(12L), any(), any())).thenReturn(2L);

        mvc.perform(get("/manutencoes/5").with(solicitante()))
                .andExpect(content().string(containsString("reserva(s) ativa(s) neste período")))
                .andExpect(content().string(containsString("não são canceladas nem alteradas automaticamente")));
    }

    @Test
    void impactoDeBloqueioConcluidoUsaOInstanteDeEncerramentoComoFim() throws Exception {
        when(service.consultar(6L)).thenReturn(concluido(6));

        mvc.perform(get("/manutencoes/6").with(admin()))
                .andExpect(content().string(containsString("vigorou até 01/06/2025 12:00")))
                .andExpect(content().string(containsString("Encerrado em")))
                .andExpect(content().string(not(containsString("Concluir manutenção"))))
                .andExpect(content().string(not(containsString("Cancelar bloqueio"))))
                .andExpect(content().string(not(containsString("/editar"))));

        ArgumentCaptor<Instant> fim = ArgumentCaptor.forClass(Instant.class);
        verify(reservas).contarReservasAtivasNoPeriodo(eq(TipoRecurso.SALA), eq(12L), any(), fim.capture());
        assertThat(fim.getValue()).isEqualTo(Instant.parse("2025-06-01T15:00:00Z"));
    }

    @Test
    void bloqueioCanceladoNaoAfetaADisponibilidadeENaoConsultaReservas() throws Exception {
        when(service.consultar(7L)).thenReturn(cancelado(7));

        mvc.perform(get("/manutencoes/7").with(admin()))
                .andExpect(content().string(containsString("não afeta a disponibilidade")))
                .andExpect(content().string(not(containsString("Cancelar bloqueio"))));

        verifyNoInteractions(reservas);
    }

    @Test
    void quemNaoEAdministradorNaoVeAcoesNoDetalhe() throws Exception {
        when(service.consultar(5L)).thenReturn(emAndamento(5));

        mvc.perform(get("/manutencoes/5").with(solicitante()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Concluir manutenção"))))
                .andExpect(content().string(not(containsString("Cancelar bloqueio"))))
                .andExpect(content().string(not(containsString("/editar"))));
    }

    @Test
    void detalheInexistenteVoltaParaAAgendaComAviso() throws Exception {
        when(service.consultar(99L)).thenThrow(new RecursoNaoEncontradoException("nao existe"));

        mvc.perform(get("/manutencoes/99").with(solicitante()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manutencoes?aviso=nao-encontrado"));
    }

    // ---------- autorização das telas de escrita ----------

    @Test
    void quemNaoEAdministradorNaoAcessaNenhumaTelaNemAcaoDeEscrita() throws Exception {
        String[] corpo = {"recurso", "SALA:12", "motivo", "Troca", "inicio", INICIO, "fim", FIM};
        mvc.perform(get("/manutencoes/novo").with(solicitante())).andExpect(status().isForbidden());
        mvc.perform(get("/manutencoes/5/editar").with(solicitante())).andExpect(status().isForbidden());
        mvc.perform(post("/manutencoes").with(solicitante()).with(csrf())
                        .param(corpo[0], corpo[1]).param(corpo[2], corpo[3]).param(corpo[4], corpo[5]).param(corpo[6], corpo[7]))
                .andExpect(status().isForbidden());
        mvc.perform(post("/manutencoes/5").with(solicitante()).with(csrf())
                        .param(corpo[0], corpo[1]).param(corpo[2], corpo[3]).param(corpo[4], corpo[5]).param(corpo[6], corpo[7]))
                .andExpect(status().isForbidden());
        mvc.perform(post("/manutencoes/5/concluir").with(solicitante()).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/manutencoes/5/cancelar").with(solicitante()).with(csrf())).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void escritaSemTokenCsrfEhRecusadaMesmoParaOAdministrador() throws Exception {
        mvc.perform(post("/manutencoes").with(admin())
                        .param("recurso", "SALA:12").param("motivo", "Troca")
                        .param("inicio", INICIO).param("fim", FIM))
                .andExpect(status().isForbidden());
        mvc.perform(post("/manutencoes/5/concluir").with(admin())).andExpect(status().isForbidden());
        mvc.perform(post("/manutencoes/5/cancelar").with(admin())).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    // ---------- registro ----------

    @Test
    void formularioDeRegistroAgrupaOsRecursosAtivosPorTipo() throws Exception {
        mvc.perform(get("/manutencoes/novo").with(admin()))
                .andExpect(status().isOk())
                .andExpect(view().name("manutencoes/formulario"))
                .andExpect(content().string(containsString("label=\"Salas\"")))
                .andExpect(content().string(containsString("label=\"Professores\"")))
                .andExpect(content().string(containsString("label=\"Materiais\"")))
                .andExpect(content().string(containsString("value=\"SALA:12\"")))
                .andExpect(content().string(containsString("value=\"PROFESSOR:7\"")))
                .andExpect(content().string(containsString("value=\"MATERIAL:3\"")))
                .andExpect(content().string(not(containsString("SALA:13"))))
                .andExpect(content().string(containsString("type=\"datetime-local\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void registroValidoGravaComOFusoDeBrasiliaERedirecionaParaODetalhe() throws Exception {
        when(service.criar(any(BloqueioDto.Entrada.class))).thenReturn(futuro(5));

        mvc.perform(post("/manutencoes").with(admin()).with(csrf())
                        .param("recurso", "SALA:12").param("motivo", "  Troca do projetor  ")
                        .param("inicio", INICIO).param("fim", FIM))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manutencoes/5"))
                .andExpect(flash().attributeExists("sucesso"));

        ArgumentCaptor<BloqueioDto.Entrada> entrada = ArgumentCaptor.forClass(BloqueioDto.Entrada.class);
        verify(service).criar(entrada.capture());
        assertThat(entrada.getValue().recursoTipo()).isEqualTo(TipoRecurso.SALA);
        assertThat(entrada.getValue().recursoId()).isEqualTo(12L);
        assertThat(entrada.getValue().motivo()).isEqualTo("Troca do projetor");
        assertThat(entrada.getValue().inicio().toInstant()).isEqualTo(Instant.parse("2099-01-10T11:00:00Z"));
        assertThat(entrada.getValue().fim().toInstant()).isEqualTo(Instant.parse("2099-01-10T21:00:00Z"));
    }

    static Stream<Arguments> entradasInvalidas() {
        String longo = "m".repeat(501);
        return Stream.of(
                arguments("", "Troca", INICIO, FIM, "Escolha o recurso."),
                arguments("FERRAMENTA:1", "Troca", INICIO, FIM, "Escolha o recurso."),
                arguments("SALA:abc", "Troca", INICIO, FIM, "Escolha o recurso."),
                arguments("SALA:0", "Troca", INICIO, FIM, "Escolha o recurso."),
                arguments("SALA:12", "", INICIO, FIM, "O motivo é obrigatório."),
                arguments("SALA:12", "   ", INICIO, FIM, "O motivo é obrigatório."),
                arguments("SALA:12", longo, INICIO, FIM, "no máximo 500 caracteres"),
                arguments("SALA:12", "Troca", "", FIM, "Informe o início do período."),
                arguments("SALA:12", "Troca", "lixo", FIM, "A data de início não é válida."),
                arguments("SALA:12", "Troca", INICIO, "", "Informe o fim do período."),
                arguments("SALA:12", "Troca", INICIO, "31/12/2099", "A data de fim não é válida."),
                arguments("SALA:12", "Troca", FIM, INICIO, "O fim deve ser posterior ao início."),
                arguments("SALA:12", "Troca", INICIO, INICIO, "O fim deve ser posterior ao início."),
                arguments("SALA:12", "Troca", "2020-01-01T08:00", "2020-01-02T08:00", "O fim deve estar no futuro.")
        );
    }

    @ParameterizedTest
    @MethodSource("entradasInvalidas")
    void entradaInvalidaVoltaAoFormularioComAMensagemENaoGrava(
            String recurso, String motivo, String inicio, String fim, String mensagem) throws Exception {
        mvc.perform(post("/manutencoes").with(admin()).with(csrf())
                        .param("recurso", recurso).param("motivo", motivo)
                        .param("inicio", inicio).param("fim", fim))
                .andExpect(status().isOk())
                .andExpect(view().name("manutencoes/formulario"))
                .andExpect(content().string(containsString(mensagem)));

        verifyNoInteractions(service);
    }

    @Test
    void oFormularioComErroMantemOQueFoiDigitado() throws Exception {
        mvc.perform(post("/manutencoes").with(admin()).with(csrf())
                        .param("recurso", "SALA:12").param("motivo", "Troca do projetor")
                        .param("inicio", FIM).param("fim", INICIO))
                .andExpect(content().string(containsString("Troca do projetor")))
                .andExpect(content().string(containsString("value=\"" + FIM + "\"")));
    }

    @Test
    void recursoQueDeixouDeExistirMostraMensagemNoCampo() throws Exception {
        when(service.criar(any(BloqueioDto.Entrada.class))).thenThrow(new RecursoNaoEncontradoException("sumiu"));

        mvc.perform(post("/manutencoes").with(admin()).with(csrf())
                        .param("recurso", "SALA:12").param("motivo", "Troca")
                        .param("inicio", INICIO).param("fim", FIM))
                .andExpect(status().isOk())
                .andExpect(view().name("manutencoes/formulario"))
                .andExpect(content().string(containsString("O recurso escolhido não existe mais.")));
    }

    // ---------- edição ----------

    @Test
    void edicaoCarregaOsDadosEMostraORecursoSemPermitirTroca() throws Exception {
        when(service.consultar(5L)).thenReturn(futuro(5));

        mvc.perform(get("/manutencoes/5/editar").with(admin()))
                .andExpect(status().isOk())
                .andExpect(view().name("manutencoes/formulario"))
                .andExpect(content().string(containsString("Editar bloqueio de manutenção")))
                .andExpect(content().string(containsString("Sala · Sala 101")))
                .andExpect(content().string(containsString("não pode ser trocado")))
                .andExpect(content().string(containsString("value=\"SALA:12\"")))
                .andExpect(content().string(containsString("value=\"" + INICIO + "\"")))
                .andExpect(content().string(containsString("value=\"" + FIM + "\"")))
                .andExpect(content().string(not(containsString("<select"))));
    }

    @Test
    void bloqueioEncerradoNaoPodeSerEditado() throws Exception {
        when(service.consultar(6L)).thenReturn(concluido(6));

        mvc.perform(get("/manutencoes/6/editar").with(admin()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manutencoes/6"))
                .andExpect(flash().attributeExists("erro"));
    }

    @Test
    void edicaoDeBloqueioInexistenteVoltaParaAAgenda() throws Exception {
        when(service.consultar(99L)).thenThrow(new RecursoNaoEncontradoException("nao existe"));

        mvc.perform(get("/manutencoes/99/editar").with(admin()))
                .andExpect(redirectedUrl("/manutencoes?aviso=nao-encontrado"));
    }

    @Test
    void atualizacaoValidaGravaERedirecionaParaODetalhe() throws Exception {
        mvc.perform(post("/manutencoes/5").with(admin()).with(csrf())
                        .param("recurso", "SALA:12").param("motivo", "Troca do projetor e do cabo")
                        .param("inicio", INICIO).param("fim", FIM))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manutencoes/5"))
                .andExpect(flash().attribute("sucesso", "Bloqueio atualizado."));

        ArgumentCaptor<BloqueioDto.Entrada> entrada = ArgumentCaptor.forClass(BloqueioDto.Entrada.class);
        verify(service).atualizar(eq(5L), entrada.capture());
        assertThat(entrada.getValue().recursoTipo()).isEqualTo(TipoRecurso.SALA);
        assertThat(entrada.getValue().recursoId()).isEqualTo(12L);
        assertThat(entrada.getValue().motivo()).isEqualTo("Troca do projetor e do cabo");
    }

    @Test
    void atualizacaoComPeriodoInvalidoVoltaAoFormularioSemGravar() throws Exception {
        mvc.perform(post("/manutencoes/5").with(admin()).with(csrf())
                        .param("recurso", "SALA:12").param("motivo", "Troca")
                        .param("inicio", FIM).param("fim", INICIO))
                .andExpect(status().isOk())
                .andExpect(view().name("manutencoes/formulario"))
                .andExpect(content().string(containsString("O fim deve ser posterior ao início.")))
                .andExpect(content().string(containsString("Sala · Sala 101")));

        verify(service, never()).atualizar(any(), any());
    }

    @Test
    void atualizacaoDeBloqueioQueJaFoiEncerradoVoltaParaODetalheComAviso() throws Exception {
        when(service.atualizar(eq(6L), any(BloqueioDto.Entrada.class)))
                .thenThrow(new ConflitoDeEstadoException("encerrado"));

        mvc.perform(post("/manutencoes/6").with(admin()).with(csrf())
                        .param("recurso", "SALA:12").param("motivo", "Troca")
                        .param("inicio", INICIO).param("fim", FIM))
                .andExpect(redirectedUrl("/manutencoes/6"))
                .andExpect(flash().attribute("erro",
                        "Este bloqueio não pode mais ser alterado: ele já foi concluído ou cancelado."));
    }

    // ---------- encerramento ----------

    @Test
    void concluirBloqueioEmAndamentoRedirecionaComMensagem() throws Exception {
        mvc.perform(post("/manutencoes/5/concluir").with(admin()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manutencoes/5"))
                .andExpect(flash().attribute("sucesso",
                        "Bloqueio concluído. O recurso volta a ficar disponível a partir de agora."));

        verify(service).concluir(5L);
    }

    @Test
    void concluirBloqueioQueAindaNaoComecouExplicaOMotivo() throws Exception {
        when(service.concluir(5L)).thenThrow(new ConflitoDeEstadoException("ainda nao comecou"));
        when(service.consultar(5L)).thenReturn(futuro(5));

        mvc.perform(post("/manutencoes/5/concluir").with(admin()).with(csrf()))
                .andExpect(redirectedUrl("/manutencoes/5"))
                .andExpect(flash().attribute("erro",
                        "O bloqueio ainda não começou: aguarde o início ou cancele-o."));
    }

    @Test
    void concluirBloqueioJaEncerradoExplicaOMotivo() throws Exception {
        when(service.concluir(6L)).thenThrow(new ConflitoDeEstadoException("encerrado"));
        when(service.consultar(6L)).thenReturn(concluido(6));

        mvc.perform(post("/manutencoes/6/concluir").with(admin()).with(csrf()))
                .andExpect(flash().attribute("erro", "Este bloqueio não está mais ativo."));
    }

    @Test
    void cancelarBloqueioAtivoRedirecionaComMensagemEMantemOHistorico() throws Exception {
        mvc.perform(post("/manutencoes/5/cancelar").with(admin()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manutencoes/5"))
                .andExpect(flash().attribute("sucesso",
                        "Bloqueio cancelado. Ele permanece no histórico e não afeta mais a disponibilidade."));

        verify(service).cancelar(5L);
    }

    @Test
    void cancelarBloqueioJaEncerradoExplicaOMotivo() throws Exception {
        when(service.cancelar(6L)).thenThrow(new ConflitoDeEstadoException("encerrado"));

        mvc.perform(post("/manutencoes/6/cancelar").with(admin()).with(csrf()))
                .andExpect(flash().attribute("erro", "Este bloqueio não está mais ativo."));
    }

    // ---------- erros e responsividade ----------

    @Test
    void erroInesperadoNaoExpoeDetalhesInternos() throws Exception {
        when(service.criar(any(BloqueioDto.Entrada.class))).thenThrow(new RuntimeException("senha=segredo-interno"));

        mvc.perform(post("/manutencoes").with(admin()).with(csrf())
                        .param("recurso", "SALA:12").param("motivo", "Troca")
                        .param("inicio", INICIO).param("fim", FIM))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(not(containsString("segredo-interno"))));
    }

    @Test
    void todasAsTelasDeclaramViewportEUsamGradeResponsiva() throws Exception {
        when(service.listar(null, null, null)).thenReturn(List.of(futuro(5)));
        when(service.consultar(5L)).thenReturn(futuro(5));

        mvc.perform(get("/manutencoes").with(admin()))
                .andExpect(content().string(containsString(VIEWPORT)))
                .andExpect(content().string(containsString("table-responsive")))
                .andExpect(content().string(containsString("col-sm-")));
        mvc.perform(get("/manutencoes/5").with(admin()))
                .andExpect(content().string(containsString(VIEWPORT)))
                .andExpect(content().string(containsString("col-sm-")));
        mvc.perform(get("/manutencoes/novo").with(admin()))
                .andExpect(content().string(containsString(VIEWPORT)))
                .andExpect(content().string(containsString("col-sm-6")));
        mvc.perform(get("/manutencoes/5/editar").with(admin()))
                .andExpect(content().string(containsString(VIEWPORT)));
    }
}