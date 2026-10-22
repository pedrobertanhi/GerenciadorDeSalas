package br.com.senac.gerenciadordesalas.salas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Transactional 
public class SalaControllerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

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
                .with(csrf()) // Adicionado token CSRF necessário após a PR #138
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

        mockMvc.perform(post("/api/salas")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SOLICITANTE")
    void solicitanteNaoDeveAcessarCrudDeSalas() throws Exception {
        mockMvc.perform(get("/api/salas"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void deveInativarSalaPreservandoHistorico() throws Exception {
        Sala sala = repository.save(new Sala("Sala C3", 20, "Bloco C"));

        mockMvc.perform(patch("/api/salas/" + sala.getId() + "/inativar")
                .with(csrf()))
                .andExpect(status().isNoContent());

        Sala salaAtualizada = repository.findById(sala.getId()).orElseThrow();
        assertFalse(salaAtualizada.getAtiva());
    }
}