package br.com.senac.gerenciadordesalas.cadastros;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Valida que o Spring/springdoc gera o documento OpenAPI (BASE-06, #10)
 * com titulo, versao, esquema de seguranca e as principais tags.
 */
class OpenApiDocumentoTest extends IntegracaoPostgresBase {

    @Test
    @WithMockUser
    void documentoOpenApi_eGeradoComMetadadosEsperados() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.openapi").value(org.hamcrest.Matchers.startsWith("3.")))
                .andExpect(jsonPath("$.info.title").value("Gerenciador de Salas"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.components.securitySchemes.sessao").exists())
                .andExpect(jsonPath("$.components.securitySchemes.sessao.type").value("apiKey"))
                .andExpect(jsonPath("$.components.securitySchemes.sessao.in").value("cookie"));
    }

    @Test
    @WithMockUser
    void documentoOpenApi_contemOperacoesDosControladoresRest() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/salas'].get").exists())
                .andExpect(jsonPath("$.paths['/api/salas'].post").exists())
                .andExpect(jsonPath("$.paths['/api/materiais'].get").exists())
                .andExpect(jsonPath("$.paths['/api/professores'].get").exists())
                .andExpect(jsonPath("$.paths['/api/disponibilidade/consulta'].get").exists());
    }

    @Test
    @WithMockUser
    void swaggerUi_eServidoNaRotaPadrao() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }
}