package br.com.senac.gerenciadordesalas.professor;

import br.com.senac.gerenciadordesalas.manutencao.MaintenanceBlockService;
import br.com.senac.gerenciadordesalas.disponibilidade.ConsultaDisponibilidadeService;
import br.com.senac.gerenciadordesalas.reservas.ReservationService;
import br.com.senac.gerenciadordesalas.reservas.ReservationManagementService;
import br.com.senac.gerenciadordesalas.material.MaterialService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Gera o documento real com a autoconfiguração do springdoc; não requer banco.
@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfessorOpenApiTest {
    @MockitoBean ConsultaDisponibilidadeService consultaDisponibilidade;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean ProfessorService professores;
    @MockitoBean MaterialService materiais;
    @MockitoBean SalaService salas;
    @MockitoBean UserRepository usuarios;
    @MockitoBean MaintenanceBlockService bloqueios;
    @MockitoBean ReservationService reservas;
    @MockitoBean ReservationManagementService gerenciamentoReservas;

    @Test void documentacaoExigeSessao() throws Exception {
        mvc.perform(get("/v3/api-docs").accept("application/json"))
                .andExpect(status().isUnauthorized());
    }

    @Test void publicaContratosReaisDosCincoEndpoints() throws Exception {
        String json = mvc.perform(get("/v3/api-docs")
                        .with(user("leitor").roles("SOLICITANTE")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode doc = mapper.readTree(json);
        JsonNode scheme = doc.at("/components/securitySchemes/sessao");
        assertThat(scheme.path("type").asText()).isEqualTo("apiKey");
        assertThat(scheme.path("in").asText()).isEqualTo("cookie");
        assertThat(scheme.path("name").asText()).isEqualTo("JSESSIONID");
        String[][] routes = {{"/api/professores", "get", "200"},
                {"/api/professores/{id}", "get", "200"},
                {"/api/professores", "post", "201"},
                {"/api/professores/{id}", "put", "200"},
                {"/api/professores/{id}/desativar", "patch", "204"}};
        for (String[] route : routes) {
            JsonNode op = doc.path("paths").path(route[0]).path(route[1]);
            assertThat(op.path("summary").asText()).isNotBlank();
            assertThat(op.path("security").get(0).has("sessao")).isTrue();
            for (String code : new String[]{route[2], "401", "403", "500"}) {
                assertThat(op.path("responses").has(code)).as(route[1] + " " + code).isTrue();
            }
            assertThat(op.path("responses").path("401").at("/content/application~1json/schema/$ref").asText())
                    .endsWith("/ErroResposta");
        }
        JsonNode paths = doc.path("paths");
        for (String method : new String[]{"post", "put"}) {
            JsonNode op = paths.path(method.equals("post") ? "/api/professores" : "/api/professores/{id}").path(method);
            assertThat(op.at("/requestBody/required").asBoolean()).isTrue();
            assertThat(op.at("/requestBody/content/application~1json/schema/$ref").asText()).endsWith("/ProfessorEntrada");
            assertThat(op.path("responses").has("400")).isTrue();
            assertThat(op.path("responses").has("409")).isTrue();
            assertThat(op.path("description").asText()).contains("ADMINISTRADOR", "X-CSRF-TOKEN");
        }
        assertThat(paths.path("/api/professores").path("get")
                .at("/responses/200/content/application~1json/schema/type").asText()).isEqualTo("array");
        assertThat(paths.path("/api/professores").path("get")
                .at("/responses/200/content/application~1json/schema/items/$ref").asText()).endsWith("/ProfessorSaida");
        assertThat(paths.path("/api/professores/{id}/desativar").path("patch")
                .at("/responses/204").has("content")).isFalse();
        for (String method : new String[]{"get", "put"}) {
            assertThat(paths.path("/api/professores/{id}").path(method).path("responses").has("404")).isTrue();
        }
        JsonNode schemas = doc.path("components").path("schemas");
        assertThat(schemas.path("ProfessorEntrada").path("required").toString()).contains("nome", "email", "competencias");
        assertThat(schemas.path("ProfessorEntrada").at("/properties/nome/maxLength").asInt()).isEqualTo(150);
        assertThat(schemas.path("ProfessorEntrada").at("/properties/email/maxLength").asInt()).isEqualTo(254);
        assertThat(schemas.path("ProfessorSaida").path("properties").has("ativo")).isTrue();
    }
}
