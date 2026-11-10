package br.com.senac.gerenciadordesalas.usuario;

import br.com.senac.gerenciadordesalas.erro.CorrelationIdFilter;
import br.com.senac.gerenciadordesalas.seguranca.RespostasDeSeguranca;
import br.com.senac.gerenciadordesalas.seguranca.SecurityConfig;
import br.com.senac.gerenciadordesalas.seguranca.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioWebController.class)
@Import({SecurityConfig.class, RespostasDeSeguranca.class, UsuarioDetailsService.class, CorrelationIdFilter.class})
@ActiveProfiles("test")
class UsuarioWebControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    UsuarioAdminService service;

    @MockitoBean
    UserRepository usuarios;

    private static RequestPostProcessor admin() {
        return user("admin@exemplo.test").roles("ADMINISTRADOR");
    }

    private static UsuarioDto.Saida conta() {
        return new UsuarioDto.Saida(3L, "Maria", "maria@exemplo.test", Role.SOLICITANTE, true, null, null);
    }

    @Test
    void listaComPaginacaoELayoutResponsivo() throws Exception {
        when(service.paginar(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(conta()), PageRequest.of(0, 10), 11));
        mvc.perform(get("/usuarios").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("maria@exemplo.test")))
                .andExpect(content().string(containsString("table-responsive")))
                .andExpect(content().string(containsString("/usuarios?page=1")));
    }

    @Test
    void cadastroValidoUsaServicoAdministrativo() throws Exception {
        var form = new UsuarioDto.Entrada("Maria", "maria@exemplo.test", "Exemplo1234", Role.SOLICITANTE);
        mvc.perform(post("/usuarios").with(admin()).with(csrf())
                        .param("nome", form.nome()).param("email", form.email())
                        .param("senha", form.senha()).param("perfil", form.perfil().name()))
                .andExpect(redirectedUrl("/usuarios"));
        verify(service).criar(eq(form), eq("admin@exemplo.test"));
    }

    @Test
    void formularioInvalidoNaoReexibeSenha() throws Exception {
        mvc.perform(post("/usuarios").with(admin()).with(csrf())
                        .param("nome", "Maria").param("email", "invalido")
                        .param("senha", "SenhaSintetica321").param("perfil", "INVALIDO"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Selecione um perfil válido.")))
                .andExpect(content().string(not(containsString("SenhaSintetica321"))));
        verifyNoInteractions(service);
    }

    @Test
    void editaSomenteNomeEPerfil() throws Exception {
        when(service.consultar(3L)).thenReturn(conta());
        mvc.perform(get("/usuarios/3/editar").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("name=\"senha\""))));
        mvc.perform(post("/usuarios/3/editar").with(admin()).with(csrf())
                        .param("nome", "Maria Souza").param("perfil", "RESPONSAVEL"))
                .andExpect(redirectedUrl("/usuarios"));
        verify(service).atualizar(3L, new UsuarioDto.Atualizacao("Maria Souza", Role.RESPONSAVEL), "admin@exemplo.test");
    }

    @Test
    void exigeConfirmacaoEProtecaoCsrfParaMudarSituacao() throws Exception {
        when(service.consultar(3L)).thenReturn(conta());
        mvc.perform(get("/usuarios/3/situacao").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Confirmar inativação")));
        mvc.perform(post("/usuarios/3/situacao").with(admin()).param("ativar", "false"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/usuarios/3/situacao").with(admin()).with(csrf()).param("ativar", "false"))
                .andExpect(redirectedUrl("/usuarios"));
        mvc.perform(post("/usuarios/3/situacao").with(admin()).with(csrf()).param("ativar", "true"))
                .andExpect(redirectedUrl("/usuarios"));
        verify(service).inativar(3L, "admin@exemplo.test");
        verify(service).ativar(3L, "admin@exemplo.test");
    }

    @ParameterizedTest
    @ValueSource(strings = {"SOLICITANTE", "RESPONSAVEL"})
    void negaConsultaEAlteracaoAOutrosPerfis(String perfil) throws Exception {
        var outroPerfil = user("outro@exemplo.test").roles(perfil);
        mvc.perform(get("/usuarios").with(outroPerfil)).andExpect(status().isForbidden());
        mvc.perform(post("/usuarios/3/situacao").with(outroPerfil).with(csrf()).param("ativar", "true"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
