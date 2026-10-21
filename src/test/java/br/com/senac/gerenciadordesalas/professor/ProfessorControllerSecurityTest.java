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
}