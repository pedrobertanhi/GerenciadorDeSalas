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
class Erro409ConflitoTest {

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
    void conflitoDeEstado_retorna409ComCorrelationIdConsistente() throws Exception {
        MvcResult result = mockMvc.perform(get("/test-erros/conflito"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_DE_ESTADO"))
                .andExpect(jsonPath("$.correlationId").exists())
                .andReturn();

        String correlationIdNoCorpo = com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.correlationId");
        String correlationIdNoHeader = result.getResponse().getHeader(CorrelationIdFilter.HEADER_CORRELATION_ID);

        assertThat(correlationIdNoHeader).isNotBlank();
        assertThat(correlationIdNoCorpo).isEqualTo(correlationIdNoHeader);
    }
}