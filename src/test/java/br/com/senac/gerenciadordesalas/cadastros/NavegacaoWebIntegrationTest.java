package br.com.senac.gerenciadordesalas.cadastros;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Menu comum das telas (ENT-03, #185): cada perfil ve so os itens que pode usar, o
 * usuario conectado aparece no cabecalho e o botao Sair leva de volta ao login.
 */
class NavegacaoWebIntegrationTest extends IntegracaoPostgresBase {

    /**
     * /professores abre para qualquer perfil e nao depende de o usuario existir no banco
     * (/reservas procura o solicitante e daria 404; /salas e so do administrador).
     */
    private String menuDe(String email, String perfil) throws Exception {
        return mockMvc.perform(get("/professores").with(user(email).roles(perfil)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void solicitante_veOsItensComunsSemAprovacoesNemUsuarios() throws Exception {
        String html = menuDe("ana@exemplo.test", "SOLICITANTE");

        assertThat(html).contains("Navegação principal", "href=\"/reservas\"",
                "href=\"/recursos/pesquisa\"", "href=\"/manutencoes\"", "href=\"/professores\"",
                "ana@exemplo.test", "Sair");
        assertThat(html).doesNotContain("href=\"/aprovacoes\"").doesNotContain("href=\"/usuarios\"")
                .doesNotContain("href=\"/salas\"");
    }

    @Test
    void responsavel_veAprovacoesMasNaoUsuarios() throws Exception {
        String html = menuDe("rui@exemplo.test", "RESPONSAVEL");

        assertThat(html).contains("href=\"/aprovacoes\"");
        assertThat(html).doesNotContain("href=\"/usuarios\"").doesNotContain("href=\"/salas\"");
    }

    @Test
    void administrador_veUsuariosMasNaoAprovacoes() throws Exception {
        String html = menuDe("adm@exemplo.test", "ADMINISTRADOR");

        assertThat(html).contains("href=\"/usuarios\"", "href=\"/salas\"");
        assertThat(html).doesNotContain("href=\"/aprovacoes\"");
    }

    @Test
    void sairPeloNavegador_voltaParaOLogin() throws Exception {
        mockMvc.perform(post("/logout").with(user("ana@exemplo.test").roles("SOLICITANTE")).with(csrf())
                        .accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?saiu"));
    }

    @Test
    void sairPelaApi_continuaRespondendo204() throws Exception {
        mockMvc.perform(post("/logout").with(user("ana@exemplo.test").roles("SOLICITANTE")).with(csrf()))
                .andExpect(status().isNoContent());
    }
}
