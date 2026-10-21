package br.com.senac.gerenciadordesalas.seguranca;

import br.com.senac.gerenciadordesalas.erro.CorrelationIdFilter;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProtegidoTestController.class)
@Import({SecurityConfig.class, RespostasDeSeguranca.class, UsuarioDetailsService.class, CorrelationIdFilter.class})
@ActiveProfiles("test")
public class FalhasSegurancaTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(roles = "SOLICITANTE")
    void deveRetornarBadRequestParaEntradaMalformadaSemExporDetalhes() throws Exception {
        // Simulando um JSON quebrado/malformado enviado para a API
        String jsonMalformado = "{ \"nome\": \"Sala 1\", \"capacidade\": \"texto-invalido-em-vez-de-numero\" ";

        mockMvc.perform(post("/api/reservas")
                .with(csrf())
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
