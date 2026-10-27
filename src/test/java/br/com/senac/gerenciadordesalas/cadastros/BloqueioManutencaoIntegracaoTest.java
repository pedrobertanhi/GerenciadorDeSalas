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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BloqueioManutencaoIntegracaoTest extends IntegracaoPostgresBase {

    private static final String PASSADO = "2020-01-01T08:00:00-03:00";
    private static final String FUTURO_INICIO = "2099-01-01T08:00:00-03:00";
    private static final String FUTURO_FIM = "2099-01-01T18:00:00-03:00";
    private static final String FIM_LONGE = "2099-12-31T18:00:00-03:00";
    private static final String TABELA = "tb_bloqueios_manutencao";

    private static String bloqueio(String tipo, long recursoId, String motivo, String inicio, String fim) {
        return """
                {"recursoTipo":"%s","recursoId":%d,"motivo":"%s","inicio":"%s","fim":"%s"}
                """.formatted(tipo, recursoId, motivo, inicio, fim);
    }

    private long idDe(MvcResult resultado) throws Exception {
        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private long criarRecurso(String rota, String json) throws Exception {
        return idDe(mockMvc.perform(post(rota)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private long criarSala() throws Exception {
        return criarRecurso("/api/salas",
                "{\"nome\":\"Sala Manutencao\",\"capacidade\":30,\"localizacao\":\"Predio Principal\"}");
    }

    private long criarMaterial() throws Exception {
        return criarRecurso("/api/materiais",
                "{\"nome\":\"Projetor\",\"tipo\":\"EQUIPAMENTO\",\"patrimonio\":\"PAT-MNT-1\","
                        + "\"quantidadeTotal\":1,\"localizacao\":\"Almoxarifado A\",\"restrito\":false}");
    }

    private long criarProfessor() throws Exception {
        return criarRecurso("/api/professores",
                "{\"nome\":\"Ana Souza\",\"email\":\"ana@example.test\",\"competencias\":[\"java\"]}");
    }

    private long criarRecursoDoTipo(String tipo) throws Exception {
        return switch (tipo) {
            case "SALA" -> criarSala();
            case "PROFESSOR" -> criarProfessor();
            default -> criarMaterial();
        };
    }

    private long criarBloqueio(String tipo, long recursoId, String inicio, String fim) throws Exception {
        return idDe(mockMvc.perform(post("/api/manutencoes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bloqueio(tipo, recursoId, "Troca do projetor", inicio, fim)))
                .andExpect(status().isCreated())
                .andReturn());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SALA", "PROFESSOR", "MATERIAL"})
    @WithMockUser(roles = "ADMINISTRADOR")
    void criarBloqueio_persisteParaCadaTipoDeRecurso(String tipo) throws Exception {
        long recursoId = criarRecursoDoTipo(tipo);

        mockMvc.perform(post("/api/manutencoes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bloqueio(tipo, recursoId, "Troca do projetor", FUTURO_INICIO, FUTURO_FIM)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recursoTipo").value(tipo))
                .andExpect(jsonPath("$.recursoId").value(recursoId))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.encerradoEm").doesNotExist());

        assertThat(contar(TABELA)).isEqualTo(1);
        assertThat(consultar("SELECT recurso_id FROM " + TABELA, Long.class)).isEqualTo(recursoId);
        assertThat(consultar("SELECT status FROM " + TABELA, String.class)).isEqualTo("ATIVO");
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void recursoInexistente_retorna404SemGravar() throws Exception {
        mockMvc.perform(post("/api/manutencoes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bloqueio("SALA", 999_999L, "Troca do projetor", FUTURO_INICIO, FUTURO_FIM)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));

        assertThat(contar(TABELA)).isZero();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void fimAnteriorAoInicio_retorna400SemGravar() throws Exception {
        long salaId = criarSala();

        mockMvc.perform(post("/api/manutencoes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bloqueio("SALA", salaId, "Troca do projetor", FUTURO_FIM, FUTURO_INICIO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"))
                .andExpect(jsonPath("$.fieldErrors[0].campo").value("periodoValido"));

        assertThat(contar(TABELA)).isZero();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void periodoQueJaTerminou_retorna400SemGravar() throws Exception {
        long salaId = criarSala();

        mockMvc.perform(post("/api/manutencoes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bloqueio("SALA", salaId, "Troca do projetor", PASSADO,
                                "2020-01-02T08:00:00-03:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].campo").value("fim"));

        assertThat(contar(TABELA)).isZero();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void motivoEmBranco_retorna400SemGravar() throws Exception {
        long salaId = criarSala();

        mockMvc.perform(post("/api/manutencoes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bloqueio("SALA", salaId, "   ", FUTURO_INICIO, FUTURO_FIM)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].campo").value("motivo"));

        assertThat(contar(TABELA)).isZero();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void tipoDeRecursoDesconhecido_retorna400() throws Exception {
        mockMvc.perform(post("/api/manutencoes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bloqueio("FERRAMENTA", 1L, "Troca do projetor", FUTURO_INICIO, FUTURO_FIM)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));

        assertThat(contar(TABELA)).isZero();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void alterarBloqueioAtivo_gravaNovosDadosENaoTrocaORecurso() throws Exception {
        long salaId = criarSala();
        long id = criarBloqueio("SALA", salaId, FUTURO_INICIO, FUTURO_FIM);

        mockMvc.perform(put("/api/manutencoes/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bloqueio("SALA", salaId, "Troca do projetor e do cabo", FUTURO_INICIO, FIM_LONGE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.motivo").value("Troca do projetor e do cabo"));

        assertThat(consultar("SELECT motivo FROM " + TABELA + " WHERE id = ?", String.class, id))
                .isEqualTo("Troca do projetor e do cabo");

        mockMvc.perform(put("/api/manutencoes/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bloqueio("SALA", salaId + 1, "Outro recurso", FUTURO_INICIO, FUTURO_FIM)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void concluirBloqueioEmAndamento_preservaOHistoricoENaoPermiteMaisAlteracoes() throws Exception {
        long salaId = criarSala();
        long id = criarBloqueio("SALA", salaId, PASSADO, FIM_LONGE);

        mockMvc.perform(patch("/api/manutencoes/{id}/concluir", id).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDO"))
                .andExpect(jsonPath("$.encerradoEm").exists());

        assertThat(consultar("SELECT status FROM " + TABELA + " WHERE id = ?", String.class, id))
                .isEqualTo("CONCLUIDO");
        assertThat(consultar("SELECT encerrado_em IS NOT NULL FROM " + TABELA + " WHERE id = ?",
                Boolean.class, id)).isTrue();
        assertThat(contar(TABELA)).isEqualTo(1);

        mockMvc.perform(get("/api/manutencoes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDO"));
        mockMvc.perform(get("/api/manutencoes").param("status", "CONCLUIDO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(patch("/api/manutencoes/{id}/concluir", id).with(csrf()))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/manutencoes/{id}/cancelar", id).with(csrf()))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/manutencoes/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bloqueio("SALA", salaId, "Outro motivo", PASSADO, FIM_LONGE)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void concluirBloqueioQueAindaNaoComecou_retorna409() throws Exception {
        long id = criarBloqueio("SALA", criarSala(), FUTURO_INICIO, FUTURO_FIM);

        mockMvc.perform(patch("/api/manutencoes/{id}/concluir", id).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_DE_ESTADO"));

        assertThat(consultar("SELECT status FROM " + TABELA + " WHERE id = ?", String.class, id))
                .isEqualTo("ATIVO");
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void cancelarBloqueioAtivo_preservaOHistoricoENaoPermiteMaisAlteracoes() throws Exception {
        long id = criarBloqueio("SALA", criarSala(), FUTURO_INICIO, FUTURO_FIM);

        mockMvc.perform(patch("/api/manutencoes/{id}/cancelar", id).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));

        assertThat(consultar("SELECT status FROM " + TABELA + " WHERE id = ?", String.class, id))
                .isEqualTo("CANCELADO");
        assertThat(contar(TABELA)).isEqualTo(1);

        mockMvc.perform(get("/api/manutencoes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
        mockMvc.perform(patch("/api/manutencoes/{id}/cancelar", id).with(csrf()))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/manutencoes/{id}/concluir", id).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void ninguemExcluiBloqueio_nemOAdministrador() throws Exception {
        long id = criarBloqueio("SALA", criarSala(), FUTURO_INICIO, FUTURO_FIM);

        mockMvc.perform(delete("/api/manutencoes/{id}", id).with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(contar(TABELA)).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void consulta_filtraPorTipoRecursoEEstadoEOrdenaDoMaisRecente() throws Exception {
        long salaId = criarSala();
        long materialId = criarMaterial();
        long salaAntigo = criarBloqueio("SALA", salaId, FUTURO_INICIO, FUTURO_FIM);
        long salaRecente = criarBloqueio("SALA", salaId, "2099-02-01T08:00:00-03:00", FIM_LONGE);
        criarBloqueio("MATERIAL", materialId, FUTURO_INICIO, FUTURO_FIM);
        mockMvc.perform(patch("/api/manutencoes/{id}/cancelar", salaAntigo).with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/manutencoes").param("recursoTipo", "SALA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(salaRecente));
        mockMvc.perform(get("/api/manutencoes").param("recursoTipo", "SALA").param("status", "ATIVO"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(salaRecente));
        mockMvc.perform(get("/api/manutencoes")
                        .param("recursoTipo", "MATERIAL").param("recursoId", String.valueOf(materialId)))
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/manutencoes").param("status", "CANCELADO"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(salaAntigo));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SOLICITANTE", "RESPONSAVEL"})
    void perfisNaoAdministrativos_consultamMasNaoGerenciam(String perfil) throws Exception {
        mockMvc.perform(get("/api/manutencoes").with(user("usuario.teste").roles(perfil)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/manutencoes")
                        .with(user("usuario.teste").roles(perfil))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bloqueio("SALA", 1L, "Troca do projetor", FUTURO_INICIO, FUTURO_FIM)))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/manutencoes/1/cancelar")
                        .with(user("usuario.teste").roles(perfil))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(contar(TABELA)).isZero();
    }

    @Test
    void semAutenticacao_recebe401() throws Exception {
        mockMvc.perform(get("/api/manutencoes"))
                .andExpect(status().isUnauthorized());
    }
}