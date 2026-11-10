package br.com.senac.gerenciadordesalas.cadastros;

import br.com.senac.gerenciadordesalas.usuario.Role;
import br.com.senac.gerenciadordesalas.usuario.User;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Administração de usuários contra PostgreSQL real (ENT-02): só o hash da senha é gravado, o e-mail é
 * único sem diferenciar maiúsculas, o último administrador ativo não sai do sistema e a sessão aberta de
 * quem foi inativado deixa de valer na requisição seguinte.
 */
class UsuarioAdminIntegracaoTest extends IntegracaoPostgresBase {

    private static final String SENHA = "Senha#Segura2026";

    @Autowired
    UserRepository usuarios;

    @Autowired
    PasswordEncoder codificador;

    private static RequestPostProcessor admin() {
        return user("admin.api@exemplo.test").roles("ADMINISTRADOR");
    }

    private User criar(String nome, String email, Role perfil) {
        return usuarios.saveAndFlush(new User(nome, email, codificador.encode(SENHA), perfil));
    }

    private MockHttpSession entrar(String email) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/login")
                        .param("email", email).param("senha", SENHA)
                        .with(csrf()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    @Test
    void cadastroPelaApiGravaSomenteOHashEORespostaNaoTrazCredencial() throws Exception {
        mockMvc.perform(post("/api/usuarios").with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Nova Pessoa\", \"email\": \"NOVA@Exemplo.test\", \"senha\": \"" + SENHA + "\", \"perfil\": \"RESPONSAVEL\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("nova@exemplo.test"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
        sincronizar();

        String hash = consultar("select senha_hash from tb_usuarios where email = ?", String.class, "nova@exemplo.test");
        assertThat(hash).startsWith("$2").isNotEqualTo(SENHA);
        assertThat(codificador.matches(SENHA, hash)).isTrue();
    }

    @Test
    void emailDuplicadoSemDiferencaDeMaiusculasRetorna409() throws Exception {
        criar("Ana", "ana@exemplo.test", Role.SOLICITANTE);

        mockMvc.perform(post("/api/usuarios").with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Outra Ana\", \"email\": \"ANA@EXEMPLO.TEST\", \"senha\": \"" + SENHA + "\", \"perfil\": \"SOLICITANTE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_DE_ESTADO"));
        sincronizar();

        assertThat(contar("tb_usuarios")).isEqualTo(1);
    }

    @Test
    void ultimoAdministradorAtivoNaoPodeSerInativadoNemRebaixado() throws Exception {
        User primeiro = criar("Admin Um", "admin.um@exemplo.test", Role.ADMINISTRADOR);

        mockMvc.perform(patch("/api/usuarios/{id}/inativar", primeiro.getId()).with(admin()).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_DE_ESTADO"));
        mockMvc.perform(put("/api/usuarios/{id}", primeiro.getId()).with(admin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Admin Um\", \"perfil\": \"SOLICITANTE\"}"))
                .andExpect(status().isConflict());
        sincronizar();
        entityManager.clear();
        assertThat(usuarios.findById(primeiro.getId()).orElseThrow().isActive()).isTrue();
        assertThat(usuarios.findById(primeiro.getId()).orElseThrow().getProfile()).isEqualTo(Role.ADMINISTRADOR);

        User segundo = criar("Admin Dois", "admin.dois@exemplo.test", Role.ADMINISTRADOR);
        mockMvc.perform(patch("/api/usuarios/{id}/inativar", primeiro.getId()).with(admin()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
        mockMvc.perform(patch("/api/usuarios/{id}/inativar", segundo.getId()).with(admin()).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void inativarNaoApagaEAExclusaoFisicaNaoExiste() throws Exception {
        User ana = criar("Ana", "ana@exemplo.test", Role.SOLICITANTE);

        mockMvc.perform(patch("/api/usuarios/{id}/inativar", ana.getId()).with(admin()).with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/usuarios/{id}", ana.getId()).with(admin()).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/usuarios").param("ativo", "false").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("ana@exemplo.test"));
        sincronizar();

        assertThat(contar("tb_usuarios")).isEqualTo(1);
    }

    @Test
    void sessaoAbertaDeUsuarioInativadoDeixaDeValerENaoEntraDeNovo() throws Exception {
        User ana = criar("Ana", "ana@exemplo.test", Role.SOLICITANTE);
        MockHttpSession sessao = entrar("ana@exemplo.test");

        mockMvc.perform(get("/api/salas").session(sessao).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/usuarios/{id}/inativar", ana.getId()).with(admin()).with(csrf()))
                .andExpect(status().isOk());
        sincronizar();
        entityManager.clear();

        mockMvc.perform(get("/api/salas").session(sessao).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
        mockMvc.perform(post("/login")
                        .param("email", "ana@exemplo.test").param("senha", SENHA)
                        .with(csrf()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
