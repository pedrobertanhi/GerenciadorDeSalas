package br.com.senac.gerenciadordesalas.salas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.senac.gerenciadordesalas.salas.Sala;
import br.com.senac.gerenciadordesalas.salas.SalaRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Garante que as alterações no banco de dados sejam revertidas após cada teste
public class SalaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SalaRepository repository;

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void deveCriarSalaComSucesso() throws Exception {
        String jsonRequest = """
                {
                    "nome": "Sala A1",
                    "capacidade": 30,
                    "localizacao": "Prédio Principal"
                }
                """;

        mockMvc.perform(post("/api/salas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome", is("Sala A1")));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void naoDeveCriarSalaComCapacidadeInvalida() throws Exception {
        String jsonRequest = """
                {
                    "nome": "Sala B2",
                    "capacidade": -5,
                    "localizacao": "Anexo"
                }
                """;

        // Valida que o sistema recusa a capacidade negativa (400 Bad Request)
        mockMvc.perform(post("/api/salas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SOLICITANTE")
    void solicitanteNaoDeveAcessarCrudDeSalas() throws Exception {
        // Valida que o perfil Solicitante recebe 403 Forbidden
        mockMvc.perform(get("/api/salas"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void deveInativarSalaPreservandoHistorico() throws Exception {
        Sala sala = repository.save(new Sala("Sala C3", 20, "Bloco C"));

        mockMvc.perform(patch("/api/salas/" + sala.getId() + "/inativar"))
                .andExpect(status().isNoContent());

        // Valida que a sala continua no banco, mas com o status ativa = false
        Sala salaAtualizada = repository.findById(sala.getId()).orElseThrow();
        assertFalse(salaAtualizada.getAtiva());
    }
}