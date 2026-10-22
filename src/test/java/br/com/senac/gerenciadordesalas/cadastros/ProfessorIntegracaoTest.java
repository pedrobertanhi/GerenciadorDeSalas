package br.com.senac.gerenciadordesalas.cadastros;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.stream.Collectors;

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

class ProfessorIntegracaoTest extends IntegracaoPostgresBase {

    private static String professor(String nome, String email, String... competencias) {
        String lista = Arrays.stream(competencias)
                .map(competencia -> "\"" + competencia + "\"")
                .collect(Collectors.joining(","));
        return "{\"nome\":\"%s\",\"email\":\"%s\",\"competencias\":[%s]}".formatted(nome, email, lista);
    }

    private long criarProfessor(String nome, String email, String... competencias) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/professores")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(professor(nome, email, competencias)))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void criarProfessor_normalizaEmailECompetenciasEPersiste() throws Exception {
        mockMvc.perform(post("/api/professores")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(professor("Ana Souza", "Ana.Souza@Example.Test", "Java", "  SQL ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ana.souza@example.test"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.competencias[0]").value("java"))
                .andExpect(jsonPath("$.competencias[1]").value("sql"));

        assertThat(contar("tb_professores")).isEqualTo(1);
        assertThat(contar("tb_competencias")).isEqualTo(2);
        assertThat(contar("tb_professor_competencias")).isEqualTo(2);
        assertThat(consultar("SELECT email FROM tb_professores", String.class))
                .isEqualTo("ana.souza@example.test");
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void competenciaCompartilhada_naoDuplicaORegistroDaCompetencia() throws Exception {
        criarProfessor("Ana Souza", "ana@example.test", "Java");
        criarProfessor("Bruno Lima", "bruno@example.test", "java");

        assertThat(contar("tb_professores")).isEqualTo(2);
        assertThat(contar("tb_competencias")).isEqualTo(1);
        assertThat(contar("tb_professor_competencias")).isEqualTo(2);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void emailDuplicado_ignoraMaiusculasERetorna409() throws Exception {
        criarProfessor("Ana Souza", "ana@example.test", "java");

        mockMvc.perform(post("/api/professores")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(professor("Outra Ana", "ANA@Example.Test", "sql")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_DE_ESTADO"));

        assertThat(contar("tb_professores")).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void emailInvalido_retorna400ComCampoEmail() throws Exception {
        mockMvc.perform(post("/api/professores")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(professor("Ana Souza", "semarroba", "java")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"))
                .andExpect(jsonPath("$.fieldErrors[0].campo").value("email"));

        assertThat(contar("tb_professores")).isZero();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void competenciasAusente_retorna400() throws Exception {
        mockMvc.perform(post("/api/professores")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana Souza\",\"email\":\"ana@example.test\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].campo").value("competencias"));

        assertThat(contar("tb_professores")).isZero();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void competenciasVazia_eAceita() throws Exception {
        mockMvc.perform(post("/api/professores")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(professor("Ana Souza", "ana@example.test")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.competencias").isEmpty());

        assertThat(contar("tb_professores")).isEqualTo(1);
        assertThat(contar("tb_professor_competencias")).isZero();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void professorInexistente_retorna404() throws Exception {
        mockMvc.perform(get("/api/professores/{id}", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void atualizarProfessor_trocaDadosECompetencias() throws Exception {
        long id = criarProfessor("Ana Souza", "ana@example.test", "java");

        mockMvc.perform(put("/api/professores/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(professor("Ana Souza Lima", "ana@example.test", "sql")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Souza Lima"))
                .andExpect(jsonPath("$.competencias", hasSize(1)))
                .andExpect(jsonPath("$.competencias[0]").value("sql"));

        assertThat(consultar("SELECT nome FROM tb_professores WHERE id = ?", String.class, id))
                .isEqualTo("Ana Souza Lima");
        assertThat(contar("tb_professores")).isEqualTo(1);
        assertThat(contar("tb_professor_competencias")).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void desativarProfessor_preservaRegistroERemoveDaListagem() throws Exception {
        long id = criarProfessor("Ana Souza", "ana@example.test", "java");

        mockMvc.perform(patch("/api/professores/{id}/desativar", id).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(consultar("SELECT ativo FROM tb_professores WHERE id = ?", Boolean.class, id)).isFalse();
        assertThat(contar("tb_professores")).isEqualTo(1);

        mockMvc.perform(get("/api/professores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/professores/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void filtroPorCompetencia_ignoraMaiusculasERetornaSoOsCompativeis() throws Exception {
        criarProfessor("Ana Souza", "ana@example.test", "java");
        criarProfessor("Bruno Lima", "bruno@example.test", "sql");

        mockMvc.perform(get("/api/professores").param("competencia", "JAVA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value("ana@example.test"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SOLICITANTE", "RESPONSAVEL"})
    void perfilSemPermissaoDeEscrita_leComSucessoMasNaoCadastra(String perfil) throws Exception {
        mockMvc.perform(get("/api/professores").with(user("usuario.teste").roles(perfil)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/professores")
                        .with(user("usuario.teste").roles(perfil))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(professor("Ana Souza", "ana@example.test", "java")))
                .andExpect(status().isForbidden());

        assertThat(contar("tb_professores")).isZero();
    }

    @Test
    void semAutenticacao_recebe401() throws Exception {
        mockMvc.perform(get("/api/professores"))
                .andExpect(status().isUnauthorized());
    }
}