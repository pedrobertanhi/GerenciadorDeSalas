package br.com.senac.gerenciadordesalas.erro;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TestErrorController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class Erro500InternoTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void erroInesperado_retorna500SemStackTrace() throws Exception {
        MvcResult result = mockMvc.perform(get("/test-erros/erro-interno"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"))
                .andExpect(jsonPath("$.correlationId").exists())
                .andReturn();

        String corpo = result.getResponse().getContentAsString();
        assertThat(corpo)
                .doesNotContain("at br.com.senac")
                .doesNotContain("at java.")
                .doesNotContain("Exception in thread")
                .doesNotContain("Caused by");
    }
}