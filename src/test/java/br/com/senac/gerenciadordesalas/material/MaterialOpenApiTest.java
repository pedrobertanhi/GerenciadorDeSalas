package br.com.senac.gerenciadordesalas.material;

import br.com.senac.gerenciadordesalas.reservas.ReservationService;

import br.com.senac.gerenciadordesalas.manutencao.MaintenanceBlockService;
import br.com.senac.gerenciadordesalas.professor.ProfessorService;
import br.com.senac.gerenciadordesalas.salas.SalaService;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MaterialOpenApiTest {

     @MockitoBean
    ReservationService reservas;

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @MockitoBean
    MaterialService materiais;

    @MockitoBean
    ProfessorService professores;

    @MockitoBean
    SalaService salas;

    @MockitoBean
    UserRepository usuarios;

    @MockitoBean
    MaintenanceBlockService bloqueios;

    @Test
    void publicaOsCincoEndpointsESeusContratos() throws Exception {
        String json = mvc.perform(get("/v3/api-docs")
                        .with(user("leitor").roles("SOLICITANTE")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode doc = mapper.readTree(json);
        String[][] rotas = {
                {"/api/materiais", "get", "200"},
                {"/api/materiais/{id}", "get", "200"},
                {"/api/materiais", "post", "201"},
                {"/api/materiais/{id}", "put", "200"},
                {"/api/materiais/{id}/desativar", "patch", "204"}
        };

        for (String[] rota : rotas) {
            JsonNode operacao = doc.path("paths").path(rota[0]).path(rota[1]);
            assertThat(operacao.path("summary").asText()).isNotBlank();
            assertThat(operacao.path("security").get(0).has("sessao")).isTrue();
            assertThat(operacao.path("responses").has(rota[2])).isTrue();
            assertThat(operacao.path("responses").has("401")).isTrue();
            assertThat(operacao.path("responses").has("403")).isTrue();
        }

        JsonNode schemas = doc.path("components").path("schemas");
        assertThat(schemas.path("MaterialEntrada").path("required").toString())
                .contains("nome", "tipo", "quantidadeTotal", "localizacao", "restrito");
        assertThat(schemas.path("MaterialEntrada")
                .at("/properties/quantidadeTotal/minimum").asInt()).isZero();
        assertThat(schemas.path("MaterialSaida").path("properties").has("ativo")).isTrue();
    }
}
