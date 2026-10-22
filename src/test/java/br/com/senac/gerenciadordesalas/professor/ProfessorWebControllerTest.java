package br.com.senac.gerenciadordesalas.professor;

import br.com.senac.gerenciadordesalas.erro.CorrelationIdFilter;
import br.com.senac.gerenciadordesalas.seguranca.RespostasDeSeguranca;
import br.com.senac.gerenciadordesalas.seguranca.SecurityConfig;
import br.com.senac.gerenciadordesalas.seguranca.UsuarioDetailsService;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfessorWebController.class)
@Import({SecurityConfig.class, RespostasDeSeguranca.class, UsuarioDetailsService.class, CorrelationIdFilter.class})
@ActiveProfiles("test")
class ProfessorWebControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ProfessorService service;
    @MockitoBean UserRepository userRepository;

    @Test void solicitanteConsultaLista() throws Exception {
        when(service.listar(null)).thenReturn(List.of());
        mvc.perform(get("/professores").with(user("solicitante").roles("SOLICITANTE")))
                .andExpect(status().isOk())
                .andExpect(view().name("professores/lista"));
    }

    @Test void solicitanteNaoAcessaFormulario() throws Exception {
        mvc.perform(get("/professores/novo").with(user("solicitante").roles("SOLICITANTE")))
                .andExpect(status().isForbidden());
    }

    @Test void administradorAbreFormulario() throws Exception {
        mvc.perform(get("/professores/novo").with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isOk())
                .andExpect(view().name("professores/formulario"));
    }

    @Test void administradorCadastraComCsrf() throws Exception {
        mvc.perform(post("/professores").with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                        .param("nome", "Ana")
                        .param("email", "ana@exemplo.test")
                        .param("competencias", "Matemática, Física"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/professores"));
        verify(service).criar(any());
    }

    @Test void entradaInvalidaNaoSalva() throws Exception {
        mvc.perform(post("/professores").with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                        .param("nome", "")
                        .param("email", "invalido")
                        .param("competencias", "Matemática"))
                .andExpect(status().isOk())
                .andExpect(view().name("professores/formulario"));
        verifyNoInteractions(service);
    }

    @Test void solicitanteNaoDesativa() throws Exception {
        mvc.perform(post("/professores/1/desativar")
                        .with(user("solicitante").roles("SOLICITANTE")).with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
