package br.com.senac.gerenciadordesalas.seguranca;

import br.com.senac.gerenciadordesalas.erro.CorrelationIdFilter;
import br.com.senac.gerenciadordesalas.usuario.Role;
import br.com.senac.gerenciadordesalas.usuario.User;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProtegidoTestController.class)
@Import({SecurityConfig.class, RespostasDeSeguranca.class, UsuarioDetailsService.class,
        CorrelationIdFilter.class})
class AutenticacaoWebTest {

    private static final String EMAIL = "ana.sintetica@exemplo.test";
    private static final String SENHA = "SenhaSintetica#2026";
    private static final String SENHA_ERRADA = "SenhaErrada#0000";
    private static final String MENSAGEM_PADRAO = "E-mail ou senha invalidos.";
    private static final PasswordEncoder CODIFICADOR = new BCryptPasswordEncoder();

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserRepository userRepository;

    private User usuario(boolean ativo) {
        User usuario = new User("Ana Sintetica", EMAIL, CODIFICADOR.encode(SENHA), Role.SOLICITANTE);
        if (!ativo) {
            usuario.deactivate();
        }
        return usuario;
    }

    private MvcResult tentarLogin(String email, String senha) throws Exception {
        return mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", email)
                        .param("senha", senha))
                .andReturn();
    }

    private void assertRespostaPadraoDeCredencialInvalida(MvcResult resultado) throws Exception {
        String corpo = resultado.getResponse().getContentAsString();
        String codigo = JsonPath.read(corpo, "$.codigo");
        String mensagem = JsonPath.read(corpo, "$.mensagem");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        assertThat(codigo).isEqualTo("CREDENCIAIS_INVALIDAS");
        assertThat(mensagem).isEqualTo(MENSAGEM_PADRAO);
        assertThat(corpo)
                .doesNotContain(SENHA)
                .doesNotContain(SENHA_ERRADA)
                .doesNotContainIgnoringCase("inativ")
                .doesNotContainIgnoringCase("disabled")
                .doesNotContain("$2a$");
    }

    @Test
    void semAutenticacao_retorna401PadronizadoComCorrelationIdConsistente() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/perfil"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"))
                .andExpect(jsonPath("$.correlationId").exists())
                .andReturn();

        String idNoCorpo = JsonPath.read(resultado.getResponse().getContentAsString(), "$.correlationId");
        String idNoHeader = resultado.getResponse().getHeader(CorrelationIdFilter.HEADER_CORRELATION_ID);
        assertThat(idNoHeader).isNotBlank();
        assertThat(idNoCorpo).isEqualTo(idNoHeader);
    }

    @Test
    void loginValido_autenticaEMantemSessaoNoServidor() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(true)));

        MvcResult login = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", EMAIL)
                        .param("senha", SENHA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.perfil").value("SOLICITANTE"))
                .andReturn();

        assertThat(login.getResponse().getContentAsString())
                .doesNotContain(SENHA)
                .doesNotContain("$2a$");

        MockHttpSession sessao = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(sessao).isNotNull();

        mockMvc.perform(get("/api/perfil").session(sessao))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void emailComMaiusculasEEspacos_eNormalizadoNoLogin() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(true)));

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", "  ANA.Sintetica@EXEMPLO.test ")
                        .param("senha", SENHA))
                .andExpect(status().isOk());
    }

    @Test
    void senhaInvalida_retorna401NeutroSemVazamento() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(true)));

        assertRespostaPadraoDeCredencialInvalida(tentarLogin(EMAIL, SENHA_ERRADA));
    }

    @Test
    void emailInexistente_retornaMesmaRespostaDeSenhaInvalida() throws Exception {
        assertRespostaPadraoDeCredencialInvalida(tentarLogin("ninguem@exemplo.test", SENHA));
    }

    @Test
    void usuarioInativo_comSenhaCorreta_recebeMesmaRespostaDeSenhaInvalida() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(false)));

        assertRespostaPadraoDeCredencialInvalida(tentarLogin(EMAIL, SENHA));
    }

    @Test
    void loginSemTokenCsrf_eRecusadoComContratoPadrao() throws Exception {
        mockMvc.perform(post("/login")
                        .param("email", EMAIL)
                        .param("senha", SENHA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    void logout_invalidaASessao() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(true)));

        MvcResult login = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("email", EMAIL)
                        .param("senha", SENHA))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession sessao = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(sessao).isNotNull();

        mockMvc.perform(post("/logout").with(csrf()).session(sessao))
                .andExpect(status().isNoContent());

        assertThat(sessao.isInvalid()).isTrue();
    }
}
