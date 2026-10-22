package br.com.senac.gerenciadordesalas.cadastros;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MaterialIntegracaoTest extends IntegracaoPostgresBase {

    private static String material(String nome, String tipo, String patrimonio, String quantidade, boolean restrito) {
        String patrimonioJson = patrimonio == null ? "null" : "\"" + patrimonio + "\"";
        return """
                {"nome":"%s","descricao":"Descricao de teste","tipo":"%s","patrimonio":%s,
                 "quantidadeTotal":%s,"localizacao":"Almoxarifado A","restrito":%s}
                """.formatted(nome, tipo, patrimonioJson, quantidade, restrito);
    }

    private long criarMaterial(String nome, String tipo, String patrimonio, String quantidade, boolean restrito)
            throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/materiais")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(material(nome, tipo, patrimonio, quantidade, restrito)))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void criarMaterial_normalizaPatrimonioEPersiste() throws Exception {
        mockMvc.perform(post("/api/materiais")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(material("Projetor", "EQUIPAMENTO", "pat-001", "2", true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patrimonio").value("PAT-001"))
                .andExpect(jsonPath("$.quantidadeTotal").value(2))
                .andExpect(jsonPath("$.restrito").value(true))
                .andExpect(jsonPath("$.ativo").value(true));

        assertThat(contar("tb_materiais")).isEqualTo(1);
        assertThat(consultar("SELECT patrimonio FROM tb_materiais", String.class)).isEqualTo("PAT-001");
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void patrimonioDuplicado_ignoraMaiusculasERetorna409() throws Exception {
        criarMaterial("Projetor", "EQUIPAMENTO", "pat-001", "1", true);

        mockMvc.perform(post("/api/materiais")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(material("Notebook", "EQUIPAMENTO", "PAT-001", "1", false)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_DE_ESTADO"));

        assertThat(contar("tb_materiais")).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void semPatrimonio_podeRepetirSemConflito() throws Exception {
        criarMaterial("Cabo HDMI", "MATERIAL", null, "10", false);
        criarMaterial("Caneta", "MATERIAL", null, "50", false);

        assertThat(contar("tb_materiais")).isEqualTo(2);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void quantidadeNegativa_retorna400ComCampoQuantidade() throws Exception {
        mockMvc.perform(post("/api/materiais")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(material("Cabo HDMI", "MATERIAL", null, "-1", false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"))
                .andExpect(jsonPath("$.fieldErrors[0].campo").value("quantidadeTotal"));

        assertThat(contar("tb_materiais")).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.5", "\"5\""})
    @WithMockUser(roles = "ADMINISTRADOR")
    void quantidadeNaoInteira_retorna400(String quantidade) throws Exception {
        mockMvc.perform(post("/api/materiais")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(material("Cabo HDMI", "MATERIAL", null, quantidade, false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));

        assertThat(contar("tb_materiais")).isZero();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void tipoInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/api/materiais")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(material("Cabo HDMI", "OUTRO", null, "1", false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));

        assertThat(contar("tb_materiais")).isZero();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void restritoAusente_retorna400ComCampoRestrito() throws Exception {
        mockMvc.perform(post("/api/materiais")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Cabo HDMI\",\"tipo\":\"MATERIAL\",\"quantidadeTotal\":1,"
                                + "\"localizacao\":\"Almoxarifado A\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].campo").value("restrito"));

        assertThat(contar("tb_materiais")).isZero();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void materialInexistente_retorna404() throws Exception {
        mockMvc.perform(get("/api/materiais/{id}", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void atualizarMaterial_gravaNovaQuantidadeERecusaPatrimonioDeOutro() throws Exception {
        long id = criarMaterial("Projetor", "EQUIPAMENTO", "PAT-001", "2", true);
        criarMaterial("Notebook", "EQUIPAMENTO", "PAT-002", "1", true);

        mockMvc.perform(put("/api/materiais/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(material("Projetor", "EQUIPAMENTO", "PAT-001", "7", true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeTotal").value(7));

        assertThat(consultar("SELECT quantidade_total FROM tb_materiais WHERE id = ?", Integer.class, id))
                .isEqualTo(7);

        mockMvc.perform(put("/api/materiais/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(material("Projetor", "EQUIPAMENTO", "PAT-002", "7", true)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void desativarMaterial_preservaRegistroERemoveDasConsultasAtivas() throws Exception {
        long id = criarMaterial("Projetor", "EQUIPAMENTO", "PAT-001", "2", true);

        mockMvc.perform(patch("/api/materiais/{id}/desativar", id).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(consultar("SELECT ativo FROM tb_materiais WHERE id = ?", Boolean.class, id)).isFalse();
        assertThat(contar("tb_materiais")).isEqualTo(1);

        mockMvc.perform(get("/api/materiais/{id}", id))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/materiais"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void filtros_porTipoEPorRestricao() throws Exception {
        criarMaterial("Projetor", "EQUIPAMENTO", "PAT-001", "2", true);
        criarMaterial("Cabo HDMI", "MATERIAL", null, "10", false);

        mockMvc.perform(get("/api/materiais").param("tipo", "EQUIPAMENTO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome").value("Projetor"));

        mockMvc.perform(get("/api/materiais").param("restrito", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome").value("Cabo HDMI"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SOLICITANTE", "RESPONSAVEL"})
    void perfilSemPermissaoDeEscrita_leComSucessoMasNaoCadastra(String perfil) throws Exception {
        mockMvc.perform(get("/api/materiais").with(user("usuario.teste").roles(perfil)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/materiais")
                        .with(user("usuario.teste").roles(perfil))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(material("Projetor", "EQUIPAMENTO", "PAT-001", "1", true)))
                .andExpect(status().isForbidden());

        assertThat(contar("tb_materiais")).isZero();
    }

    @Test
    void semAutenticacao_recebe401() throws Exception {
        mockMvc.perform(get("/api/materiais"))
                .andExpect(status().isUnauthorized());
    }
}