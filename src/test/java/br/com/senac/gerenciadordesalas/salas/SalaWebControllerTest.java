package br.com.senac.gerenciadordesalas.salas;

import br.com.senac.gerenciadordesalas.erro.CorrelationIdFilter;
import br.com.senac.gerenciadordesalas.seguranca.RespostasDeSeguranca;
import br.com.senac.gerenciadordesalas.seguranca.SecurityConfig;
import br.com.senac.gerenciadordesalas.seguranca.UsuarioDetailsService;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
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
 * Telas de salas (RES-01, #15) renderizadas de verdade, com a segurança real: autorização por
 * perfil, validação dos campos, mensagens, paginação, escape de HTML e marcação responsiva.
 */
@WebMvcTest(SalaWebController.class)
@Import({SecurityConfig.class, RespostasDeSeguranca.class, UsuarioDetailsService.class, CorrelationIdFilter.class})
@ActiveProfiles("test")
class SalaWebControllerTest {

    private static final String VIEWPORT = "width=device-width, initial-scale=1";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    SalaService service;

    @MockitoBean
    UserRepository userRepository;

    private static RequestPostProcessor admin() {
        return user("admin").roles("ADMINISTRADOR");
    }

    private static RequestPostProcessor solicitante() {
        return user("solicitante").roles("SOLICITANTE");
    }

    private static Sala sala() {
        Sala sala = new Sala("Sala 101", 30, "Bloco A");
        sala.setId(1L);
        return sala;
    }

    private void listaCom(SalaResponseDTO... salas) {
        when(service.pesquisar(any(SalaFiltroDTO.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(salas), PageRequest.of(0, 10), salas.length));
    }

    private static SalaResponseDTO dto(String nome) {
        return new SalaResponseDTO(1L, nome, 30, "Bloco A", true);
    }

    // ---------- listagem ----------

    @ParameterizedTest
    @ValueSource(strings = {"SOLICITANTE", "RESPONSAVEL", "ADMINISTRADOR"})
    void todosOsPerfisConsultamALista(String perfil) throws Exception {
        listaCom(dto("Sala 101"));

        mvc.perform(get("/salas").with(user("leitor").roles(perfil)))
                .andExpect(status().isOk())
                .andExpect(view().name("salas/lista"))
                .andExpect(content().string(containsString("Sala 101")))
                .andExpect(content().string(containsString("Detalhes")));
    }

    @Test
    void administradorVeAsAcoesDeEscrita() throws Exception {
        listaCom(dto("Sala 101"));

        mvc.perform(get("/salas").with(admin()))
                .andExpect(content().string(containsString("Cadastrar sala")))
                .andExpect(content().string(containsString("/salas/1/editar")))
                .andExpect(content().string(containsString("/salas/1/inativar")));
    }

    @Test
    void quemNaoEAdministradorNaoVeAsAcoesDeEscrita() throws Exception {
        listaCom(dto("Sala 101"));

        mvc.perform(get("/salas").with(solicitante()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Cadastrar sala"))))
                .andExpect(content().string(not(containsString("/editar"))))
                .andExpect(content().string(not(containsString("/inativar"))));
    }

    @Test
    void visitanteSemLoginVaiParaALogin() throws Exception {
        mvc.perform(get("/salas").header("Accept", "text/html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(service);
    }

    @Test
    void listaSemSalasMostraMensagemVazia() throws Exception {
        listaCom();

        mvc.perform(get("/salas").with(solicitante()))
                .andExpect(content().string(containsString("Nenhuma sala encontrada.")));
    }

    @Test
    void paginacaoApareceQuandoHaMaisDeUmaPagina() throws Exception {
        when(service.pesquisar(any(SalaFiltroDTO.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto("Sala 101")), PageRequest.of(0, 10), 25));

        mvc.perform(get("/salas").with(solicitante()))
                .andExpect(content().string(containsString("Página 1 de 3")))
                .andExpect(content().string(containsString("page=1")));
    }

    @Test
    void semPaginacaoQuandoHaApenasUmaPagina() throws Exception {
        listaCom(dto("Sala 101"));

        mvc.perform(get("/salas").with(solicitante()))
                .andExpect(content().string(not(containsString("Paginação das salas"))));
    }

    @Test
    void filtrosSaoRepassadosAoServicoComOTamanhoDePaginaFixo() throws Exception {
        listaCom(dto("Sala 101"));

        mvc.perform(get("/salas").param("capacidadeMinima", "20").param("localizacao", " Bloco ")
                        .param("page", "2").with(solicitante()))
                .andExpect(status().isOk());

        ArgumentCaptor<SalaFiltroDTO> filtro = ArgumentCaptor.forClass(SalaFiltroDTO.class);
        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(service).pesquisar(filtro.capture(), pagina.capture());
        assertThat(filtro.getValue().capacidadeMinima()).isEqualTo(20);
        assertThat(filtro.getValue().localizacao()).isEqualTo("Bloco");
        assertThat(pagina.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pagina.getValue().getPageSize()).isEqualTo(SalaWebController.TAMANHO_PAGINA);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-3", "abc", "1.5"})
    void capacidadeMinimaInvalidaMostraMensagemEIgnoraOFiltro(String valor) throws Exception {
        listaCom(dto("Sala 101"));

        mvc.perform(get("/salas").param("capacidadeMinima", valor).with(solicitante()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("maior que zero")));

        ArgumentCaptor<SalaFiltroDTO> filtro = ArgumentCaptor.forClass(SalaFiltroDTO.class);
        verify(service).pesquisar(filtro.capture(), any(Pageable.class));
        assertThat(filtro.getValue().capacidadeMinima()).isNull();
    }

    @Test
    void avisoDeSalaNaoEncontradaSoAceitaOValorConhecido() throws Exception {
        listaCom();

        mvc.perform(get("/salas").param("aviso", "nao-encontrada").with(solicitante()))
                .andExpect(content().string(containsString("Sala não encontrada ou já inativada.")));
        mvc.perform(get("/salas").param("aviso", "<script>alert(1)</script>").with(solicitante()))
                .andExpect(content().string(not(containsString("alert(1)"))));
    }

    @Test
    void nomeComHtmlEExibidoComEscape() throws Exception {
        listaCom(dto("<script>alert(1)</script>"));

        mvc.perform(get("/salas").with(solicitante()))
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
                .andExpect(content().string(containsString("&lt;script&gt;")));
    }

    // ---------- detalhe ----------

    @Test
    void detalheMostraOsDadosDaSala() throws Exception {
        when(service.buscarPorId(1L)).thenReturn(sala());

        mvc.perform(get("/salas/1").with(solicitante()))
                .andExpect(status().isOk())
                .andExpect(view().name("salas/detalhe"))
                .andExpect(content().string(containsString("Sala 101")))
                .andExpect(content().string(containsString("30 pessoas")))
                .andExpect(content().string(containsString("Bloco A")))
                .andExpect(content().string(not(containsString("/editar"))));
    }

    @Test
    void salaInexistenteOuInativaVoltaParaAListaComAviso() throws Exception {
        when(service.buscarPorId(99L)).thenThrow(new EntityNotFoundException("nao existe"));

        mvc.perform(get("/salas/99").with(solicitante()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/salas?aviso=nao-encontrada"));
    }

    // ---------- autorização das telas de escrita ----------

    @Test
    void quemNaoEAdministradorNaoAcessaAsTelasDeEscrita() throws Exception {
        mvc.perform(get("/salas/novo").with(solicitante())).andExpect(status().isForbidden());
        mvc.perform(get("/salas/1/editar").with(solicitante())).andExpect(status().isForbidden());
        mvc.perform(get("/salas/1/inativar").with(solicitante())).andExpect(status().isForbidden());
        mvc.perform(post("/salas").with(solicitante()).with(csrf())
                        .param("nome", "Sala X").param("capacidade", "10").param("localizacao", "Bloco X"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/salas/1").with(solicitante()).with(csrf())
                        .param("nome", "Sala X").param("capacidade", "10").param("localizacao", "Bloco X"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/salas/1/inativar").with(solicitante()).with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void escritaSemTokenCsrfEhRecusadaMesmoParaOAdministrador() throws Exception {
        mvc.perform(post("/salas").with(admin())
                        .param("nome", "Sala X").param("capacidade", "10").param("localizacao", "Bloco X"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/salas/1/inativar").with(admin()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    // ---------- cadastro ----------

    @Test
    void administradorAbreOFormularioDeCadastroComTokenCsrf() throws Exception {
        mvc.perform(get("/salas/novo").with(admin()))
                .andExpect(status().isOk())
                .andExpect(view().name("salas/formulario"))
                .andExpect(content().string(containsString("Cadastrar sala")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("name=\"nome\"")))
                .andExpect(content().string(containsString("name=\"capacidade\"")))
                .andExpect(content().string(containsString("name=\"localizacao\"")));
    }

    @Test
    void cadastroValidoGravaComOsCamposSemEspacosNasPontas() throws Exception {
        mvc.perform(post("/salas").with(admin()).with(csrf())
                        .param("nome", "  Sala 202  ").param("capacidade", " 40 ")
                        .param("localizacao", " Bloco B "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/salas"))
                .andExpect(flash().attribute("sucesso", "Sala cadastrada com sucesso."));

        ArgumentCaptor<Sala> gravada = ArgumentCaptor.forClass(Sala.class);
        verify(service).criar(gravada.capture());
        assertThat(gravada.getValue().getNome()).isEqualTo("Sala 202");
        assertThat(gravada.getValue().getCapacidade()).isEqualTo(40);
        assertThat(gravada.getValue().getLocalizacao()).isEqualTo("Bloco B");
    }

    static Stream<Arguments> entradasInvalidas() {
        return Stream.of(
                arguments("", "30", "Bloco A", "O nome da sala é obrigatório."),
                arguments("   ", "30", "Bloco A", "O nome da sala é obrigatório."),
                arguments("Sala X", "", "Bloco A", "A capacidade é obrigatória."),
                arguments("Sala X", "0", "Bloco A", "A capacidade deve ser um número inteiro positivo."),
                arguments("Sala X", "-5", "Bloco A", "A capacidade deve ser um número inteiro positivo."),
                arguments("Sala X", "abc", "Bloco A", "A capacidade deve ser um número inteiro positivo."),
                arguments("Sala X", "1.5", "Bloco A", "A capacidade deve ser um número inteiro positivo."),
                arguments("Sala X", "1000000", "Bloco A", "A capacidade deve ser um número inteiro positivo."),
                arguments("Sala X", "30", "", "A localização é obrigatória."),
                arguments("Sala X", "30", "   ", "A localização é obrigatória."),
                arguments("a".repeat(101), "30", "Bloco A", "no máximo 100 caracteres"),
                arguments("Sala X", "30", "b".repeat(256), "no máximo 255 caracteres")
        );
    }

    @ParameterizedTest
    @MethodSource("entradasInvalidas")
    void entradaInvalidaVoltaAoFormularioComAMensagemENaoGrava(
            String nome, String capacidade, String localizacao, String mensagem) throws Exception {
        mvc.perform(post("/salas").with(admin()).with(csrf())
                        .param("nome", nome).param("capacidade", capacidade).param("localizacao", localizacao))
                .andExpect(status().isOk())
                .andExpect(view().name("salas/formulario"))
                .andExpect(content().string(containsString(mensagem)));

        verifyNoInteractions(service);
    }

    @Test
    void nomeDuplicadoMostraMensagemNoCampoENaoRedireciona() throws Exception {
        when(service.criar(any(Sala.class))).thenThrow(new EntityExistsException("duplicada"));

        mvc.perform(post("/salas").with(admin()).with(csrf())
                        .param("nome", "Sala 101").param("capacidade", "30").param("localizacao", "Bloco A"))
                .andExpect(status().isOk())
                .andExpect(view().name("salas/formulario"))
                .andExpect(content().string(containsString("Já existe uma sala com este nome.")));
    }

    // ---------- edição ----------

    @Test
    void editarCarregaOsDadosAtuaisNoFormulario() throws Exception {
        when(service.buscarPorId(1L)).thenReturn(sala());

        mvc.perform(get("/salas/1/editar").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Editar sala")))
                .andExpect(content().string(containsString("value=\"Sala 101\"")))
                .andExpect(content().string(containsString("value=\"30\"")))
                .andExpect(content().string(containsString("action=\"/salas/1\"")));
    }

    @Test
    void atualizacaoValidaGravaERedireciona() throws Exception {
        mvc.perform(post("/salas/1").with(admin()).with(csrf())
                        .param("nome", "Sala 101-A").param("capacidade", "35").param("localizacao", "Bloco A"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/salas"))
                .andExpect(flash().attribute("sucesso", "Sala atualizada com sucesso."));

        verify(service).buscarPorId(1L);
        verify(service).atualizar(eq(1L), any(Sala.class));
    }

    @Test
    void atualizarSalaInexistenteVoltaParaAListaSemGravar() throws Exception {
        when(service.buscarPorId(99L)).thenThrow(new EntityNotFoundException("nao existe"));

        mvc.perform(post("/salas/99").with(admin()).with(csrf())
                        .param("nome", "Sala X").param("capacidade", "10").param("localizacao", "Bloco X"))
                .andExpect(redirectedUrl("/salas?aviso=nao-encontrada"));

        verify(service, never()).atualizar(any(), any());
    }

    // ---------- inativação ----------

    @Test
    void confirmacaoDeInativacaoMostraASalaEPedeConfirmacao() throws Exception {
        when(service.buscarPorId(1L)).thenReturn(sala());

        mvc.perform(get("/salas/1/inativar").with(admin()))
                .andExpect(status().isOk())
                .andExpect(view().name("salas/confirmar-inativacao"))
                .andExpect(content().string(containsString("Sala 101")))
                .andExpect(content().string(containsString("histórico")))
                .andExpect(content().string(containsString("Confirmar inativação")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void inativacaoConfirmadaInativaERedireciona() throws Exception {
        mvc.perform(post("/salas/1/inativar").with(admin()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/salas"))
                .andExpect(flash().attribute("sucesso",
                        "Sala inativada. O histórico de reservas foi preservado."));

        verify(service).inativar(1L);
    }

    @Test
    void inativarSalaInexistenteVoltaParaAListaSemInativar() throws Exception {
        when(service.buscarPorId(99L)).thenThrow(new EntityNotFoundException("nao existe"));

        mvc.perform(post("/salas/99/inativar").with(admin()).with(csrf()))
                .andExpect(redirectedUrl("/salas?aviso=nao-encontrada"));

        verify(service, never()).inativar(any());
    }

    // ---------- erros e responsividade ----------

    @Test
    void erroInesperadoNaoExpoeDetalhesInternos() throws Exception {
        when(service.criar(any(Sala.class))).thenThrow(new RuntimeException("senha=segredo-interno"));

        mvc.perform(post("/salas").with(admin()).with(csrf())
                        .param("nome", "Sala X").param("capacidade", "10").param("localizacao", "Bloco X"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(not(containsString("segredo-interno"))));
    }

    @Test
    void todasAsTelasDeclaramViewportEUsamGradeResponsiva() throws Exception {
        listaCom(dto("Sala 101"));
        when(service.buscarPorId(1L)).thenReturn(sala());

        mvc.perform(get("/salas").with(admin()))
                .andExpect(content().string(containsString(VIEWPORT)))
                .andExpect(content().string(containsString("table-responsive")))
                .andExpect(content().string(containsString("col-sm-")));
        mvc.perform(get("/salas/1").with(admin()))
                .andExpect(content().string(containsString(VIEWPORT)))
                .andExpect(content().string(containsString("col-sm-")));
        mvc.perform(get("/salas/novo").with(admin()))
                .andExpect(content().string(containsString(VIEWPORT)));
        mvc.perform(get("/salas/1/editar").with(admin()))
                .andExpect(content().string(containsString(VIEWPORT)));
        mvc.perform(get("/salas/1/inativar").with(admin()))
                .andExpect(content().string(containsString(VIEWPORT)));
    }
}