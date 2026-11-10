package br.com.senac.gerenciadordesalas.seguranca;

import br.com.senac.gerenciadordesalas.erro.CorrelationIdFilter;
import br.com.senac.gerenciadordesalas.usuario.Role;
import br.com.senac.gerenciadordesalas.usuario.User;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import br.com.senac.gerenciadordesalas.usuario.UsuarioAdminService;
import br.com.senac.gerenciadordesalas.usuario.UsuarioController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Conta inativa não acessa rota protegida nem com sessão existente (ENT-02), pela cadeia de segurança
 * real: o filtro derruba a sessão e a requisição segue como anônima.
 */
@WebMvcTest(UsuarioController.class)
@Import({SecurityConfig.class, RespostasDeSeguranca.class, UsuarioDetailsService.class, CorrelationIdFilter.class})
@ActiveProfiles("test")
class UsuarioInativoSessaoWebTest {

    private static final String EMAIL = "admin.sessao@exemplo.test";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    UsuarioAdminService service;

    @MockitoBean
    UserRepository userRepository;

    private static User administrador(boolean ativo) {
        User usuario = new User("Admin", EMAIL, "hash", Role.ADMINISTRADOR);
        if (!ativo) {
            usuario.deactivate();
        }
        return usuario;
    }

    @Test
    void contaInativaNaoAcessaRotaProtegidaMesmoComSessaoEPerfilCorreto() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(administrador(false)));

        mvc.perform(get("/api/usuarios").accept(MediaType.APPLICATION_JSON).with(user(EMAIL).roles("ADMINISTRADOR")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));

        verifyNoInteractions(service);
    }

    @Test
    void contaInativaNoNavegadorVaiParaALogin() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(administrador(false)));

        mvc.perform(get("/api/usuarios").accept(MediaType.TEXT_HTML).with(user(EMAIL).roles("ADMINISTRADOR")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(service);
    }

    @Test
    void contaAtivaContinuaAcessando() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(administrador(true)));
        when(service.listar(null, null)).thenReturn(List.of());

        mvc.perform(get("/api/usuarios").accept(MediaType.APPLICATION_JSON).with(user(EMAIL).roles("ADMINISTRADOR")))
                .andExpect(status().isOk());
    }

    @Test
    void identidadeSemRegistroNoBancoNaoEhDerrubadaPeloFiltro() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(service.listar(null, null)).thenReturn(List.of());

        mvc.perform(get("/api/usuarios").accept(MediaType.APPLICATION_JSON).with(user(EMAIL).roles("ADMINISTRADOR")))
                .andExpect(status().isOk());
    }
}
