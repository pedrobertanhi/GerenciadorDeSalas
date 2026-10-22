package br.com.senac.gerenciadordesalas.material;

import br.com.senac.gerenciadordesalas.erro.CorrelationIdFilter;
import br.com.senac.gerenciadordesalas.seguranca.RespostasDeSeguranca;
import br.com.senac.gerenciadordesalas.seguranca.SecurityConfig;
import br.com.senac.gerenciadordesalas.seguranca.UsuarioDetailsService;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MaterialController.class)
@Import({SecurityConfig.class, RespostasDeSeguranca.class, UsuarioDetailsService.class, CorrelationIdFilter.class})
@ActiveProfiles("test")
class MaterialControllerSecurityTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    MaterialService service;

    @MockitoBean
    UserRepository userRepository;

    @ParameterizedTest
    @ValueSource(strings = {"SOLICITANTE", "RESPONSAVEL", "ADMINISTRADOR"})
    void tresPerfisPodemConsultarMateriais(String perfil) throws Exception {
        when(service.listar(null, null)).thenReturn(List.of());

        mvc.perform(get("/api/materiais").with(user("leitor").roles(perfil)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/materiais/1").with(user("leitor").roles(perfil)))
                .andExpect(status().isOk());
    }

    @Test
    void rotasAnonimasSaoRecusadas() throws Exception {
        mvc.perform(get("/api/materiais").accept("application/json"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/materiais/1").accept("application/json"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SOLICITANTE", "RESPONSAVEL"})
    void perfisNaoAdministrativosNaoPodemAlterar(String perfil) throws Exception {
        mvc.perform(post("/api/materiais")
                        .with(user("operador").roles(perfil)).with(csrf())
                        .contentType("application/json").content(jsonValido()))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/materiais/1")
                        .with(user("operador").roles(perfil)).with(csrf())
                        .contentType("application/json").content(jsonValido()))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/materiais/1/desativar")
                        .with(user("operador").roles(perfil)).with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void administradorPrecisaDeCsrfParaAlterar() throws Exception {
        mvc.perform(post("/api/materiais")
                        .with(user("admin").roles("ADMINISTRADOR"))
                        .contentType("application/json").content(jsonValido()))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/materiais/1")
                        .with(user("admin").roles("ADMINISTRADOR"))
                        .contentType("application/json").content(jsonValido()))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/materiais/1/desativar")
                        .with(user("admin").roles("ADMINISTRADOR")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void administradorPodeExecutarCrudComCsrf() throws Exception {
        mvc.perform(post("/api/materiais")
                        .with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                        .contentType("application/json").content(jsonValido()))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/materiais/1")
                        .with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                        .contentType("application/json").content(jsonValido()))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/materiais/1/desativar")
                        .with(user("admin").roles("ADMINISTRADOR")).with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).criar(any());
        verify(service).atualizar(anyLong(), any());
        verify(service).desativar(1L);
    }

    @Test
    void quantidadeNegativaRetorna400SemEfeitoColateral() throws Exception {
        mvc.perform(post("/api/materiais")
                        .with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                        .contentType("application/json")
                        .content(jsonValido().replace("\"quantidadeTotal\":2", "\"quantidadeTotal\":-1")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void quantidadeFracionariaRetorna400SemTruncar() throws Exception {
        mvc.perform(post("/api/materiais")
                        .with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                        .contentType("application/json")
                        .content(jsonValido().replace("\"quantidadeTotal\":2", "\"quantidadeTotal\":1.5")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    private String jsonValido() {
        return """
                {
                  "nome":"Projetor",
                  "descricao":"Projetor HDMI",
                  "tipo":"EQUIPAMENTO",
                  "patrimonio":"PAT-01",
                  "quantidadeTotal":2,
                  "localizacao":"Almoxarifado",
                  "restrito":true
                }
                """;
    }
}
