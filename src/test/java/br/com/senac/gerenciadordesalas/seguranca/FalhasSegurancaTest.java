package br.com.senac.gerenciadordesalas.seguranca;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class FalhasSegurancaTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void deveRetornarBadRequestParaEntradaMalformadaSemExporDetalhes() throws Exception {
        // Simulando um JSON quebrado/malformado enviado para a API
        String jsonMalformado = "{ \"nome\": \"Sala 1\", \"capacidade\": \"texto-invalido-em-vez-de-numero\" ";

        mockMvc.perform(post("/api/salas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMalformado))
                .andExpect(status().isBadRequest())
                // Garante que a resposta não contém partes de código ou exceções internas do Java/Spring
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("java.lang"))))
                .andExpect(content().string(not(containsString("br.com.senac"))))
                .andExpect(content().string(not(containsString("StackTrace"))));
    }
}