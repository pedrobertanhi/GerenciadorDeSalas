package br.com.senac.gerenciadordesalas.erro;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TestErrorController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, CorrelationIdFilter.class})
class ErroDeEntidadePersistenciaTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    CorrelationIdFilter correlationIdFilter;

    MockMvc mockMvc;

    @BeforeEach
    void montarMockMvcComFiltroDeCorrelationId() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilter(correlationIdFilter)
                .build();
    }

    @Test
    void entidadeInexistente_retorna404SemVazarMensagemInterna() throws Exception {
        MvcResult result = mockMvc.perform(get("/test-erros/entidade-inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.correlationId").exists())
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("Entidade de teste inexistente");
    }

    @Test
    void entidadeJaExistente_retorna409SemVazarMensagemInterna() throws Exception {
        MvcResult result = mockMvc.perform(get("/test-erros/entidade-existente"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_DE_ESTADO"))
                .andExpect(jsonPath("$.correlationId").exists())
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("Entidade de teste ja existe");
    }
}