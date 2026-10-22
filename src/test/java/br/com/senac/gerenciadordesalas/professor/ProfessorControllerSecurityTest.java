package br.com.senac.gerenciadordesalas.professor;

import br.com.senac.gerenciadordesalas.seguranca.SecurityConfig;
import br.com.senac.gerenciadordesalas.seguranca.RespostasDeSeguranca;
import br.com.senac.gerenciadordesalas.seguranca.UsuarioDetailsService;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import br.com.senac.gerenciadordesalas.erro.CorrelationIdFilter;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

@WebMvcTest(ProfessorController.class)
@Import({SecurityConfig.class, RespostasDeSeguranca.class, UsuarioDetailsService.class, CorrelationIdFilter.class})
@ActiveProfiles("test")
class ProfessorControllerSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean ProfessorService service;
    @MockitoBean UserRepository userRepository;

    @Test void solicitanteNaoPodeCadastrar() throws Exception {
        mvc.perform(post("/api/professores").with(user("solicitante").roles("SOLICITANTE")).with(csrf())
                .contentType("application/json")
                .content("{\"nome\":\"Ana\",\"email\":\"ana@exemplo.test\",\"competencias\":[]}"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test void administradorPodeCadastrar() throws Exception {
        mvc.perform(post("/api/professores").with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                .contentType("application/json")
                .content("{\"nome\":\"Ana\",\"email\":\"ana@exemplo.test\",\"competencias\":[]}"))
            .andExpect(status().isCreated());
        verify(service).criar(any());
    }

    @Test void solicitantePodeConsultar() throws Exception {
        when(service.listar(null)).thenReturn(java.util.List.of());
        mvc.perform(get("/api/professores").with(user("solicitante").roles("SOLICITANTE")))
            .andExpect(status().isOk());
    }

    @Test void responsavelNaoPodeDesativar() throws Exception {
        mvc.perform(patch("/api/professores/1/desativar")
                .with(user("responsavel").roles("RESPONSAVEL")).with(csrf()))
            .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"SOLICITANTE", "RESPONSAVEL", "ADMINISTRADOR"})
    void tresPerfisPodemLer(String role) throws Exception {
        mvc.perform(get("/api/professores").with(user("leitor").roles(role)))
            .andExpect(status().isOk());
        mvc.perform(get("/api/professores/1").with(user("leitor").roles(role)))
            .andExpect(status().isOk());
    }

    @Test void leiturasAnonimasSaoNegadas() throws Exception {
        mvc.perform(get("/api/professores").accept("application/json"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/professores/1").accept("application/json"))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"SOLICITANTE", "RESPONSAVEL"})
    void escritasDeOutrosPerfisSaoNegadasMesmoComCsrf(String role) throws Exception {
        for (var request : java.util.List.of(post("/api/professores"), put("/api/professores/1"),
                patch("/api/professores/1/desativar"))) {
            mvc.perform(request.with(user("operador").roles(role)).with(csrf())
                    .contentType("application/json")
                    .content("{\"nome\":\"Exemplo\",\"email\":\"exemplo@example.test\",\"competencias\":[]}"))
                .andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
    }

    @Test void administradorPrecisaDeCsrfNasTresEscritas() throws Exception {
        for (var request : java.util.List.of(post("/api/professores"), put("/api/professores/1"),
                patch("/api/professores/1/desativar"))) {
            mvc.perform(request.with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
    }

    @Test void administradorPodeAtualizarEDesativarComCsrf() throws Exception {
        mvc.perform(put("/api/professores/1").with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                .contentType("application/json")
                .content("{\"nome\":\"Exemplo\",\"email\":\"exemplo@example.test\",\"competencias\":[]}"))
            .andExpect(status().isOk());
        mvc.perform(patch("/api/professores/1/desativar")
                .with(user("admin").roles("ADMINISTRADOR")).with(csrf()))
            .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).atualizar(eq(1L), any());
        verify(service).desativar(1L);
    }
}