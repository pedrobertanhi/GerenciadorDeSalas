package br.com.senac.gerenciadordesalas.seguranca;

import br.com.senac.gerenciadordesalas.erro.CorrelationIdFilter;
import br.com.senac.gerenciadordesalas.usuario.Role;
import br.com.senac.gerenciadordesalas.usuario.User;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProtegidoTestController.class)
@Import({SecurityConfig.class, RespostasDeSeguranca.class, UsuarioDetailsService.class,
        CorrelationIdFilter.class, LimitadorTentativasLogin.class,
        FiltroLimiteTentativasLogin.class})
class LimiteTentativasLoginWebTest {

    private static final String EMAIL = "ana.limite@exemplo.test";
    private static final String SENHA = "SenhaSintetica#2026";
    private static final String SENHA_ERRADA = "SenhaErrada#0000";
    private static final String ORIGEM = "192.0.2.55";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    LimitadorTentativasLogin limitador;

    @MockitoBean
    UserRepository userRepository;

    @BeforeEach
    void limparEstado() {
        limitador.limpar();
    }

    @Test
    void sextaTentativaEhRecusadaCom429ERetryAfter() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario()));

        for (int tentativa = 0; tentativa < 5; tentativa++) {
            mockMvc.perform(login(EMAIL, SENHA_ERRADA, ORIGEM))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"));
        }

        String corpo = mockMvc.perform(login(EMAIL, SENHA_ERRADA, ORIGEM))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.codigo").value("TENTATIVAS_EXCEDIDAS"))
                .andExpect(jsonPath("$.mensagem")
                        .value("Nao foi possivel autenticar. Tente novamente mais tarde."))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(corpo)
                .doesNotContain(EMAIL)
                .doesNotContain(SENHA)
                .doesNotContain(SENHA_ERRADA)
                .doesNotContainIgnoringCase("usuario");
    }

    @Test
    void contaExistenteEInexistenteRecebemRespostaInicialIndistinguivel() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario()));

        String respostaExistente = mockMvc.perform(login(EMAIL, SENHA_ERRADA, "192.0.2.10"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String respostaInexistente = mockMvc.perform(
                        login("ninguem@exemplo.test", SENHA_ERRADA, "192.0.2.11"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(respostaExistente)
                .contains("\"codigo\":\"CREDENCIAIS_INVALIDAS\"",
                        "\"mensagem\":\"E-mail ou senha invalidos.\"");
        assertThat(respostaInexistente)
                .contains("\"codigo\":\"CREDENCIAIS_INVALIDAS\"",
                        "\"mensagem\":\"E-mail ou senha invalidos.\"");
    }

    @Test
    void loginValidoReiniciaContagem() throws Exception {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario()));

        for (int tentativa = 0; tentativa < 4; tentativa++) {
            mockMvc.perform(login(EMAIL, SENHA_ERRADA, ORIGEM))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(login(EMAIL, SENHA, ORIGEM))
                .andExpect(status().isOk());

        for (int tentativa = 0; tentativa < 4; tentativa++) {
            mockMvc.perform(login(EMAIL, SENHA_ERRADA, ORIGEM))
                    .andExpect(status().isUnauthorized());
        }
    }

    private MockHttpServletRequestBuilder login(String email, String senha, String origem) {
        return post("/login")
                .with(csrf())
                .with(request -> {
                    request.setRemoteAddr(origem);
                    return request;
                })
                .param("email", email)
                .param("senha", senha);
    }

    private User usuario() {
        return new User("Ana Limite", EMAIL,
                new BCryptPasswordEncoder().encode(SENHA), Role.SOLICITANTE);
    }
}
