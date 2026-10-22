package br.com.senac.gerenciadordesalas.salas;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SalaIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SalaRepository salaRepository;

    @AfterEach
    void tearDown() {
        // Garante que o banco em memória começa limpo para cada teste
        salaRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void pesquisarSemResultados_deveRetornarPaginaVazia() throws Exception {
        mockMvc.perform(get("/api/salas")
                        .param("sort", "nome,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void pesquisar_deveAplicarFiltrosDeCapacidadeCorretamente() throws Exception {
        
        // ATENÇÃO: Ajuste a instanciação abaixo conforme a estrutura da sua entidade Sala
        // Se usar @Builder (ex: Sala.builder().nome("X").build()) ou tiver um construtor obrigatório, altere aqui.
        Sala salaPequena = new Sala();
        salaPequena.setNome("Sala Alpha");
        salaPequena.setCapacidade(20);
        salaPequena.setLocalizacao("Anexo");
        salaRepository.save(salaPequena);

        Sala salaGrande = new Sala();
        salaGrande.setNome("Sala Beta");
        salaGrande.setCapacidade(50);
        salaGrande.setLocalizacao("Principal");
        salaRepository.save(salaGrande);

        // Pesquisa filtrando por capacidade mínima de 40 (deve retornar apenas a Sala Beta)
        mockMvc.perform(get("/api/salas")
                        .param("capacidadeMinima", "40")
                        .param("sort", "nome,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].nome").value("Sala Beta"));
    }
}