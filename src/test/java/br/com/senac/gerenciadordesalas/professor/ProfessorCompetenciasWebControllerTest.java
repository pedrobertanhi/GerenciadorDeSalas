package br.com.senac.gerenciadordesalas.professor;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import br.com.senac.gerenciadordesalas.erro.CorrelationIdFilter;
import br.com.senac.gerenciadordesalas.erro.RecursoNaoEncontradoException;
import br.com.senac.gerenciadordesalas.seguranca.RespostasDeSeguranca;
import br.com.senac.gerenciadordesalas.seguranca.SecurityConfig;
import br.com.senac.gerenciadordesalas.seguranca.UsuarioDetailsService;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
 * Telas de professores e competencias (ENT-03, #185): incluir, remover, mensagens de erro com o
 * texto digitado preservado, estados vazio e de falha e bloqueio para quem nao e administrador.
 */
@WebMvcTest(ProfessorWebController.class)
@Import({SecurityConfig.class, RespostasDeSeguranca.class, UsuarioDetailsService.class, CorrelationIdFilter.class})
@ActiveProfiles("test")
class ProfessorCompetenciasWebControllerTest {

    private static final ProfessorDto.Saida ANA =
            new ProfessorDto.Saida(1L, "Ana Souza", "ana@exemplo.test", true, List.of("matemática"));

    @Autowired MockMvc mvc;
    @MockitoBean ProfessorService service;
    @MockitoBean UserRepository userRepository;

    @BeforeEach
    void configurar() {
        when(service.consultar(1L)).thenReturn(ANA);
    }

    @Test
    void administradorVeAsCompetenciasDoProfessor() throws Exception {
        mvc.perform(get("/professores/1/competencias").with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(view().name("professores/competencias"))
                .andExpect(content().string(containsString("Ana Souza")))
                .andExpect(content().string(containsString("matemática")))
                .andExpect(content().string(containsString("Incluir competência")));
    }

    @Test
    void incluirCompetencia_voltaComMensagemDeSucesso() throws Exception {
        mvc.perform(post("/professores/1/competencias").with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                        .param("competencia", "Química"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/professores/1/competencias"))
                .andExpect(flash().attribute("sucesso", "Competência incluída."));
        verify(service).adicionarCompetencia(1L, "Química");
    }

    @Test
    void competenciaRepetida_mostraErroEPreservaODigitado() throws Exception {
        when(service.adicionarCompetencia(eq(1L), anyString()))
                .thenThrow(new ConflitoDeEstadoException("O professor já possui a competência \"matemática\"."));

        mvc.perform(post("/professores/1/competencias").with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                        .param("competencia", "Matemática"))
                .andExpect(status().isOk())
                .andExpect(view().name("professores/competencias"))
                .andExpect(content().string(containsString("O professor já possui a competência")))
                .andExpect(content().string(containsString("value=\"Matemática\"")));
    }

    @Test
    void competenciaVazia_mostraErro() throws Exception {
        when(service.adicionarCompetencia(eq(1L), anyString()))
                .thenThrow(new IllegalArgumentException("Informe a competência."));

        mvc.perform(post("/professores/1/competencias").with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                        .param("competencia", "  "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Informe a competência.")));
    }

    @Test
    void removerCompetencia_voltaComMensagemDeSucesso() throws Exception {
        mvc.perform(post("/professores/1/competencias/remover").with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                        .param("competencia", "matemática"))
                .andExpect(redirectedUrl("/professores/1/competencias"))
                .andExpect(flash().attribute("sucesso", "Competência removida."));
        verify(service).removerCompetencia(1L, "matemática");
    }

    @Test
    void removerCompetenciaAusente_voltaComErro() throws Exception {
        when(service.removerCompetencia(eq(1L), anyString()))
                .thenThrow(new ConflitoDeEstadoException("O professor não possui a competência \"física\"."));

        mvc.perform(post("/professores/1/competencias/remover").with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                        .param("competencia", "física"))
                .andExpect(redirectedUrl("/professores/1/competencias"))
                .andExpect(flash().attribute("erro", "O professor não possui a competência \"física\"."));
    }

    @Test
    void solicitanteNaoAlteraCompetencias() throws Exception {
        mvc.perform(get("/professores/1/competencias").with(user("ana").roles("SOLICITANTE")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/professores/1/competencias").with(user("ana").roles("SOLICITANTE")).with(csrf())
                        .param("competencia", "química"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/professores/1/competencias/remover").with(user("ana").roles("RESPONSAVEL")).with(csrf())
                        .param("competencia", "matemática"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    // Valores sem acento: o th:field escreve acentos como entidades HTML (F&iacute;sica).
    @Test
    void cadastroInvalido_mostraMensagensEmPortuguesEPreservaOsCampos() throws Exception {
        mvc.perform(post("/professores").with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                        .param("nome", "")
                        .param("email", "invalido")
                        .param("competencias", "Fisica"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Informe o nome.")))
                .andExpect(content().string(containsString("Informe um e-mail válido.")))
                .andExpect(content().string(containsString("value=\"invalido\"")))
                .andExpect(content().string(containsString("value=\"Fisica\"")));
        verifyNoInteractions(service);
    }

    @Test
    void cadastroSemCompetencias_eAceito() throws Exception {
        mvc.perform(post("/professores").with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                        .param("nome", "Bruno")
                        .param("email", "bruno@exemplo.test")
                        .param("competencias", ""))
                .andExpect(redirectedUrl("/professores"));
        verify(service).criar(new ProfessorDto.Entrada("Bruno", "bruno@exemplo.test", java.util.Set.of()));
    }

    @Test
    void competenciaEmBrancoNoMeioDaLista_eRecusada() throws Exception {
        mvc.perform(post("/professores").with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                        .param("nome", "Bruno")
                        .param("email", "bruno@exemplo.test")
                        .param("competencias", "Fisica, , Quimica"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Informe competências válidas de até 100 caracteres.")))
                .andExpect(content().string(containsString("value=\"Fisica, , Quimica\"")));
        verifyNoInteractions(service);
    }

    @Test
    void listaVazia_eFiltroSemResultado_temMensagensProprias() throws Exception {
        when(service.listar(any())).thenReturn(List.of());

        mvc.perform(get("/professores").with(user("ana").roles("SOLICITANTE")))
                .andExpect(content().string(containsString("Nenhum professor cadastrado.")));
        mvc.perform(get("/professores").param("competencia", "astronomia").with(user("ana").roles("SOLICITANTE")))
                .andExpect(content().string(containsString("Nenhum professor com essa competência.")));
    }

    @Test
    void falhaNoBanco_mostraAvisoSemQuebrarATela() throws Exception {
        when(service.listar(any())).thenThrow(new DataAccessResourceFailureException("banco fora"));

        mvc.perform(get("/professores").with(user("ana").roles("SOLICITANTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Não foi possível carregar os professores agora.")))
                .andExpect(content().string(not(containsString("Nenhum professor cadastrado."))))
                .andExpect(content().string(not(containsString("banco fora"))));
    }

    @Test
    void desativarProfessorInexistente_voltaComErro() throws Exception {
        doThrow(new RecursoNaoEncontradoException("Professor nao encontrado")).when(service).desativar(9L);

        mvc.perform(post("/professores/9/desativar").with(user("admin").roles("ADMINISTRADOR")).with(csrf()))
                .andExpect(redirectedUrl("/professores"))
                .andExpect(flash().attribute("erro", "Professor não encontrado."));
    }

    @Test
    void administradorVeOAtalhoDeCompetenciasNaLista() throws Exception {
        when(service.listar(any())).thenReturn(List.of(ANA));

        mvc.perform(get("/professores").with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(content().string(containsString("href=\"/professores/1/competencias\"")));
        mvc.perform(get("/professores").with(user("ana").roles("SOLICITANTE")))
                .andExpect(content().string(not(containsString("href=\"/professores/1/competencias\""))));
    }
}
