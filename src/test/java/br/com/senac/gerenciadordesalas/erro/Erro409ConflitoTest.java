package br.com.senac.gerenciadordesalas.erro;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TestErrorController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class Erro409ConflitoTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void conflitoDeEstado_retorna409() throws Exception {
        mockMvc.perform(get("/test-erros/conflito"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_DE_ESTADO"))
                .andExpect(jsonPath("$.correlationId").exists());
    }
}