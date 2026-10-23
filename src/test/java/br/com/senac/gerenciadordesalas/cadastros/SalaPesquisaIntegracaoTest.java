package br.com.senac.gerenciadordesalas.cadastros;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pesquisa combinada e paginada de salas (RES-05, #19) com massa controlada:
 * cinco salas conhecidas, sendo uma inativa, criadas antes de cada teste.
 */
@WithMockUser(roles = "ADMINISTRADOR")
class SalaPesquisaIntegracaoTest extends IntegracaoPostgresBase {

    @BeforeEach
    void criarMassaControlada() throws Exception {
        criarSala("Auditorio", 120, "Bloco A - Terreo");
        criarSala("Laboratorio 1", 30, "Bloco B - 2o andar");
        criarSala("Sala 101", 40, "Bloco A - 1o andar");
        criarSala("Sala 102", 25, "Bloco A - 1o andar");
        long inativa = criarSala("Sala Antiga", 60, "Bloco A - Terreo");
        mockMvc.perform(patch("/api/salas/{id}/inativar", inativa).with(csrf()))
                .andExpect(status().isNoContent());
    }

    private long criarSala(String nome, int capacidade, String localizacao) throws Exception {
        String json = """
                {"nome":"%s","capacidade":%d,"localizacao":"%s"}
                """.formatted(nome, capacidade, localizacao);
        MvcResult resultado = mockMvc.perform(post("/api/salas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    @Test
    void semFiltros_retornaSomenteAtivasOrdenadasPorNome() throws Exception {
        mockMvc.perform(get("/api/salas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[*].nome",
                        contains("Auditorio", "Laboratorio 1", "Sala 101", "Sala 102")));
    }

    @Test
    void filtroVazio_equivaleAAusenciaDeFiltro() throws Exception {
        mockMvc.perform(get("/api/salas").param("localizacao", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    void capacidadeMinima_incluiOLimiteEExcluiMenores() throws Exception {
        mockMvc.perform(get("/api/salas").param("capacidadeMinima", "40"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].nome", contains("Auditorio", "Sala 101")));
    }

    @Test
    void localizacao_buscaParcialSemDiferenciarMaiusculas() throws Exception {
        mockMvc.perform(get("/api/salas").param("localizacao", "  bloco a  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].nome",
                        contains("Auditorio", "Sala 101", "Sala 102")));
    }

    @Test
    void filtrosCombinados_aplicamTodasAsCondicoes() throws Exception {
        mockMvc.perform(get("/api/salas")
                        .param("capacidadeMinima", "30")
                        .param("localizacao", "1o andar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].nome", contains("Sala 101")));
    }

    @Test
    void salaInativaNaoApareceMesmoAtendendoAosFiltros() throws Exception {
        mockMvc.perform(get("/api/salas")
                        .param("capacidadeMinima", "50")
                        .param("localizacao", "terreo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].nome", contains("Auditorio")));
    }

    @Test
    void semResultado_retornaPaginaVaziaCom200() throws Exception {
        mockMvc.perform(get("/api/salas")
                        .param("capacidadeMinima", "500")
                        .param("localizacao", "Bloco Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void paginacao_respeitaTamanhoENumeroDaPagina() throws Exception {
        mockMvc.perform(get("/api/salas").param("size", "2").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].nome", contains("Sala 101", "Sala 102")))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void ordenacao_porCapacidadeDecrescente() throws Exception {
        mockMvc.perform(get("/api/salas").param("sort", "capacidade,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].nome",
                        contains("Auditorio", "Sala 101", "Laboratorio 1", "Sala 102")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void capacidadeMinimaInvalida_retorna400(String capacidade) throws Exception {
        mockMvc.perform(get("/api/salas").param("capacidadeMinima", capacidade))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"));
    }
}