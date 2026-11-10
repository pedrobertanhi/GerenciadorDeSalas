package br.com.senac.gerenciadordesalas.usuario;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import br.com.senac.gerenciadordesalas.erro.CorrelationIdFilter;
import br.com.senac.gerenciadordesalas.seguranca.RespostasDeSeguranca;
import br.com.senac.gerenciadordesalas.seguranca.SecurityConfig;
import br.com.senac.gerenciadordesalas.seguranca.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autorização, validação e respostas da API de usuários (ENT-02) com a segurança real: só o
 * administrador administra, a exclusão física não existe e nenhuma resposta traz credencial.
 */
@WebMvcTest(UsuarioController.class)
@Import({SecurityConfig.class, RespostasDeSeguranca.class, UsuarioDetailsService.class, CorrelationIdFilter.class})
@ActiveProfiles("test")
class UsuarioControllerSecurityTest {

    private static final String SENHA_VALIDA = "Senha#Segura2026";
    private static final String CORPO_VALIDO = corpo("Maria Souza", "maria@exemplo.test", SENHA_VALIDA, "RESPONSAVEL");

    @Autowired
    MockMvc mvc;

    @MockitoBean
    UsuarioAdminService service;

    @MockitoBean
    UserRepository userRepository;

    private static RequestPostProcessor admin() {
        return user("admin").roles("ADMINISTRADOR");
    }

    private static String corpo(String nome, String email, String senha, String perfil) {
        return """
                {"nome": "%s", "email": "%s", "senha": "%s", "perfil": "%s"}
                """.formatted(nome, email, senha, perfil);
    }

    private static UsuarioDto.Saida saida() {
        return new UsuarioDto.Saida(7L, "Maria Souza", "maria@exemplo.test", Role.RESPONSAVEL, true,
                Instant.parse("2026-11-10T12:00:00Z"), Instant.parse("2026-11-10T12:00:00Z"));
    }

    // ---------- administrador ----------

    @Test
    void administradorCadastraEARespostaNaoTrazNenhumaCredencial() throws Exception {
        when(service.criar(any(UsuarioDto.Entrada.class), eq("admin"))).thenReturn(saida());

        mvc.perform(post("/api/usuarios").with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.email").value("maria@exemplo.test"))
                .andExpect(jsonPath("$.perfil").value("RESPONSAVEL"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(content().string(not(containsString(SENHA_VALIDA))));

        verify(service).criar(any(UsuarioDto.Entrada.class), eq("admin"));
    }

    @Test
    void administradorListaEConsultaComFiltros() throws Exception {
        when(service.listar(Role.RESPONSAVEL, true)).thenReturn(List.of(saida()));
        when(service.consultar(7L)).thenReturn(saida());

        mvc.perform(get("/api/usuarios").param("perfil", "RESPONSAVEL").param("ativo", "true").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].senha").doesNotExist());
        mvc.perform(get("/api/usuarios/7").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Souza"));
    }

    @Test
    void administradorAtualizaInativaEReativa() throws Exception {
        when(service.atualizar(eq(7L), any(UsuarioDto.Atualizacao.class), eq("admin"))).thenReturn(saida());
        when(service.inativar(7L, "admin")).thenReturn(saida());
        when(service.ativar(7L, "admin")).thenReturn(saida());

        mvc.perform(put("/api/usuarios/7").with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\": \"Maria\", \"perfil\": \"SOLICITANTE\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/usuarios/7/inativar").with(admin()).with(csrf())).andExpect(status().isOk());
        mvc.perform(patch("/api/usuarios/7/ativar").with(admin()).with(csrf())).andExpect(status().isOk());
    }

    // ---------- autorização ----------

    @ParameterizedTest
    @ValueSource(strings = {"SOLICITANTE", "RESPONSAVEL"})
    void outrosPerfisNaoAcessamNenhumaRotaDeUsuarios(String perfil) throws Exception {
        RequestPostProcessor operador = user("operador").roles(perfil);

        mvc.perform(get("/api/usuarios").with(operador)).andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios/7").with(operador)).andExpect(status().isForbidden());
        mvc.perform(post("/api/usuarios").with(operador).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO)).andExpect(status().isForbidden());
        mvc.perform(put("/api/usuarios/7").with(operador).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\": \"X\", \"perfil\": \"ADMINISTRADOR\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/usuarios/7/inativar").with(operador).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(patch("/api/usuarios/7/ativar").with(operador).with(csrf())).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void semSessaoTodasAsRotasDevolvem401() throws Exception {
        mvc.perform(get("/api/usuarios").accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/usuarios").with(csrf()).accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO)).andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void exclusaoFisicaNaoExisteNemParaOAdministrador() throws Exception {
        mvc.perform(delete("/api/usuarios/7").with(admin()).with(csrf())).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void escritasSemTokenCsrfSaoRecusadasMesmoParaOAdministrador() throws Exception {
        mvc.perform(post("/api/usuarios").with(admin())
                .contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO)).andExpect(status().isForbidden());
        mvc.perform(put("/api/usuarios/7").with(admin())
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\": \"X\", \"perfil\": \"SOLICITANTE\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/usuarios/7/inativar").with(admin())).andExpect(status().isForbidden());
        mvc.perform(patch("/api/usuarios/7/ativar").with(admin())).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    // ---------- validação ----------

    static Stream<Arguments> corposInvalidos() {
        return Stream.of(
                arguments("senha curta", corpo("Maria", "maria@exemplo.test", "Abc123", "SOLICITANTE")),
                arguments("senha sem número", corpo("Maria", "maria@exemplo.test", "SomenteLetrasAqui", "SOLICITANTE")),
                arguments("senha sem letra", corpo("Maria", "maria@exemplo.test", "12345678901234", "SOLICITANTE")),
                arguments("senha com acento", corpo("Maria", "maria@exemplo.test", "Senha#Segura2026ç", "SOLICITANTE")),
                arguments("senha acima de 72", corpo("Maria", "maria@exemplo.test", "A1" + "x".repeat(71), "SOLICITANTE")),
                arguments("e-mail inválido", corpo("Maria", "isto-nao-e-email", SENHA_VALIDA, "SOLICITANTE")),
                arguments("nome em branco", corpo("   ", "maria@exemplo.test", SENHA_VALIDA, "SOLICITANTE")),
                arguments("perfil inexistente", corpo("Maria", "maria@exemplo.test", SENHA_VALIDA, "SUPERUSUARIO")),
                arguments("perfil ausente", "{\"nome\": \"Maria\", \"email\": \"maria@exemplo.test\", \"senha\": \"" + SENHA_VALIDA + "\"}"),
                arguments("json quebrado", "{\"nome\": ")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("corposInvalidos")
    void cadastroInvalidoDevolve400SemRepetirASenhaERecebida(String caso, String corpo) throws Exception {
        mvc.perform(post("/api/usuarios").with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString(SENHA_VALIDA))))
                .andExpect(content().string(not(containsString("Abc123"))))
                .andExpect(content().string(not(containsString("12345678901234"))));

        verifyNoInteractions(service);
    }

    @Test
    void identificadorOuFiltroComFormatoInvalidoDevolve400() throws Exception {
        mvc.perform(get("/api/usuarios/abc").with(admin())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/usuarios").param("perfil", "SUPERUSUARIO").with(admin())).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/usuarios/abc/inativar").with(admin()).with(csrf())).andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    // ---------- erros de negócio ----------

    @Test
    void conflitoDeNegocioVira409ComCodigoEstavelESemDetalheInterno() throws Exception {
        when(service.inativar(7L, "admin"))
                .thenThrow(new ConflitoDeEstadoException("Nao e possivel inativar o ultimo administrador ativo"));

        mvc.perform(patch("/api/usuarios/7/inativar").with(admin()).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_DE_ESTADO"))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("at br.com"))));
    }
}
