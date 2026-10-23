package br.com.senac.gerenciadordesalas.cadastros;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SalaIntegracaoTest extends IntegracaoPostgresBase {

    private static String sala(String nome, Object capacidade, String localizacao) {
        return """
                {"nome":"%s","capacidade":%s,"localizacao":"%s"}
                """.formatted(nome, capacidade, localizacao);
    }

    private long criarSala(String nome) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/salas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sala(nome, 30, "Predio Principal")))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void criarSala_persisteERetorna201ComLocation() throws Exception {
        mockMvc.perform(post("/api/salas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sala("Sala A1", 30, "Predio Principal")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/salas/")))
                .andExpect(jsonPath("$.nome").value("Sala A1"))
                .andExpect(jsonPath("$.capacidade").value(30))
                .andExpect(jsonPath("$.ativa").value(true));

        assertThat(contar("salas")).isEqualTo(1);
        assertThat(consultar("SELECT nome FROM salas", String.class)).isEqualTo("Sala A1");
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void nomeDuplicado_retorna409SemCriarSegundoRegistro() throws Exception {
        criarSala("Sala Duplicada");

        mockMvc.perform(post("/api/salas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sala("Sala Duplicada", 10, "Anexo")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_DE_ESTADO"));

        assertThat(contar("salas")).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -5})
    @WithMockUser(roles = "ADMINISTRADOR")
    void capacidadeInvalida_retorna400ComCampoCapacidade(int capacidade) throws Exception {
        mockMvc.perform(post("/api/salas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sala("Sala Invalida", capacidade, "Anexo")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"))
                .andExpect(jsonPath("$.fieldErrors[0].campo").value("capacidade"));

        assertThat(contar("salas")).isZero();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void nomeEmBranco_retorna400ComCampoNome() throws Exception {
        mockMvc.perform(post("/api/salas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sala("   ", 10, "Anexo")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].campo").value("nome"));

        assertThat(contar("salas")).isZero();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void salaInexistente_retorna404() throws Exception {
        mockMvc.perform(get("/api/salas/{id}", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void atualizarSala_gravaNovosDadosERecusaNomeDeOutraSala() throws Exception {
        long idA = criarSala("Sala Original");
        criarSala("Sala Ocupada");

        mockMvc.perform(put("/api/salas/{id}", idA)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sala("Sala Renomeada", 50, "Bloco B")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Sala Renomeada"));

        assertThat(consultar("SELECT nome FROM salas WHERE id = ?", String.class, idA))
                .isEqualTo("Sala Renomeada");
        assertThat(consultar("SELECT capacidade FROM salas WHERE id = ?", Integer.class, idA))
                .isEqualTo(50);

        mockMvc.perform(put("/api/salas/{id}", idA)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sala("Sala Ocupada", 50, "Bloco B")))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void inativarSala_preservaRegistroEHistorico() throws Exception {
        long id = criarSala("Sala Para Inativar");

        mockMvc.perform(patch("/api/salas/{id}/inativar", id).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(consultar("SELECT ativa FROM salas WHERE id = ?", Boolean.class, id)).isFalse();
        assertThat(contar("salas")).isEqualTo(1);

        mockMvc.perform(get("/api/salas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativa").value(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SOLICITANTE", "RESPONSAVEL"})
    void perfilSemPermissaoDeEscrita_recebe403MasPodePesquisar(String perfil) throws Exception {
        mockMvc.perform(post("/api/salas")
                        .with(user("usuario.teste").roles(perfil))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sala("Sala Proibida", 10, "Anexo")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/salas").with(user("usuario.teste").roles(perfil)))
                .andExpect(status().isOk());

        assertThat(contar("salas")).isZero();
    }

    @Test
    void semAutenticacao_recebe401() throws Exception {
        mockMvc.perform(get("/api/salas"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/salas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sala("Sala Anonima", 10, "Anexo")))
                .andExpect(status().isUnauthorized());

        assertThat(contar("salas")).isZero();
    }
}