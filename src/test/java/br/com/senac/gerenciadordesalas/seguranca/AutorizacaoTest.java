package br.com.senac.gerenciadordesalas.seguranca;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public class AutorizacaoTest {

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

    // --- Testes de Acesso Permitido (Cenários Positivos) ---

    @Test
    @WithMockUser(roles = "SOLICITANTE")
    void solicitanteDeveAcessarRotasPermitidas() throws Exception {
        mockMvc.perform(post("/api/reservas").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "matheus.moura@senac.br", roles = "SOLICITANTE")
    void solicitanteDeveAcessarPropriaReserva() throws Exception {
        mockMvc.perform(get("/api/reservas/minhas/matheus.moura@senac.br"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RESPONSAVEL")
    void responsavelDeveAprovarReservas() throws Exception {
        mockMvc.perform(post("/api/reservas/1/aprovar").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorDeveAcessarRotasAdministrativas() throws Exception {
        mockMvc.perform(post("/api/admin/usuarios").with(csrf()))
                .andExpect(status().isOk());
    }

    // --- Testes de Acesso Negado (Cenários Negativos - 403 Forbidden) ---

    @Test
    @WithMockUser(roles = "SOLICITANTE")
    void solicitanteNaoDeveAcessarRotasDeAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/usuarios").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "matheus.moura@senac.br", roles = "SOLICITANTE")
    void solicitanteNaoDeveAcessarReservaDeOutroUsuario() throws Exception {
        mockMvc.perform(get("/api/reservas/minhas/outro.usuario@senac.br"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RESPONSAVEL")
    void responsavelNaoDeveCriarUsuariosAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/usuarios").with(csrf()))
                .andExpect(status().isForbidden());
    }

    // --- Testes de Acesso Anônimo (401 Unauthorized) ---

    @Test
    void usuarioNaoAutenticadoNaoDeveAcessarRotasProtegidas() throws Exception {
        mockMvc.perform(get("/api/disponibilidade"))
                .andExpect(status().isUnauthorized());
    }
}