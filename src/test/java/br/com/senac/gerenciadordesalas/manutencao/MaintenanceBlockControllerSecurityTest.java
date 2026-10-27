package br.com.senac.gerenciadordesalas.manutencao;

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

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MaintenanceBlockController.class)
@Import({SecurityConfig.class, RespostasDeSeguranca.class, UsuarioDetailsService.class, CorrelationIdFilter.class})
@ActiveProfiles("test")
class MaintenanceBlockControllerSecurityTest {

    private static final String JSON_VALIDO = """
            {"recursoTipo":"SALA","recursoId":12,"motivo":"Troca do projetor",
             "inicio":"2099-01-01T08:00:00-03:00","fim":"2099-01-01T18:00:00-03:00"}
            """;

    private static final String JSON_PERIODO_INVERTIDO = """
            {"recursoTipo":"SALA","recursoId":12,"motivo":"Troca do projetor",
             "inicio":"2099-01-01T18:00:00-03:00","fim":"2099-01-01T08:00:00-03:00"}
            """;

    @Autowired
    MockMvc mvc;

    @MockitoBean
    MaintenanceBlockService service;

    @MockitoBean
    UserRepository userRepository;

    private BloqueioDto.Saida saida() {
        return new BloqueioDto.Saida(1L, TipoRecurso.SALA, 12L, "Troca do projetor",
                OffsetDateTime.parse("2099-01-01T08:00:00-03:00"),
                OffsetDateTime.parse("2099-01-01T18:00:00-03:00"),
                StatusBloqueio.ATIVO, null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SOLICITANTE", "RESPONSAVEL", "ADMINISTRADOR"})
    void tresPerfisPodemConsultarBloqueios(String perfil) throws Exception {
        when(service.listar(null, null, null)).thenReturn(List.of(saida()));
        when(service.consultar(1L)).thenReturn(saida());

        mvc.perform(get("/api/manutencoes").with(user("leitor").roles(perfil)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/manutencoes/1").with(user("leitor").roles(perfil)))
                .andExpect(status().isOk());
    }

    @Test
    void rotasAnonimasSaoRecusadas() throws Exception {
        mvc.perform(get("/api/manutencoes").accept("application/json"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/manutencoes").with(csrf()).accept("application/json")
                        .contentType("application/json").content(JSON_VALIDO))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SOLICITANTE", "RESPONSAVEL"})
    void perfisNaoAdministrativosNaoGerenciamBloqueios(String perfil) throws Exception {
        mvc.perform(post("/api/manutencoes")
                        .with(user("operador").roles(perfil)).with(csrf())
                        .contentType("application/json").content(JSON_VALIDO))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/manutencoes/1")
                        .with(user("operador").roles(perfil)).with(csrf())
                        .contentType("application/json").content(JSON_VALIDO))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/manutencoes/1/concluir")
                        .with(user("operador").roles(perfil)).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/manutencoes/1/cancelar")
                        .with(user("operador").roles(perfil)).with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void administradorGerenciaBloqueios() throws Exception {
        when(service.criar(any(BloqueioDto.Entrada.class))).thenReturn(saida());
        when(service.atualizar(org.mockito.ArgumentMatchers.eq(1L), any(BloqueioDto.Entrada.class)))
                .thenReturn(saida());
        when(service.concluir(1L)).thenReturn(saida());
        when(service.cancelar(1L)).thenReturn(saida());

        mvc.perform(post("/api/manutencoes")
                        .with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                        .contentType("application/json").content(JSON_VALIDO))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/manutencoes/1")
                        .with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                        .contentType("application/json").content(JSON_VALIDO))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/manutencoes/1/concluir")
                        .with(user("admin").roles("ADMINISTRADOR")).with(csrf()))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/manutencoes/1/cancelar")
                        .with(user("admin").roles("ADMINISTRADOR")).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void ninguemExcluiBloqueiosNemOAdministrador() throws Exception {
        mvc.perform(delete("/api/manutencoes/1")
                        .with(user("admin").roles("ADMINISTRADOR")).with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void periodoInvertidoEhRecusadoComCampoPeriodoValido() throws Exception {
        mvc.perform(post("/api/manutencoes")
                        .with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                        .contentType("application/json").content(JSON_PERIODO_INVERTIDO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_INVALIDA"))
                .andExpect(jsonPath("$.fieldErrors[0].campo").value("periodoValido"));

        verifyNoInteractions(service);
    }

    @Test
    void motivoEmBrancoEhRecusadoComCampoMotivo() throws Exception {
        String semMotivo = JSON_VALIDO.replace("Troca do projetor", "   ");

        mvc.perform(post("/api/manutencoes")
                        .with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                        .contentType("application/json").content(semMotivo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].campo").value("motivo"));

        verifyNoInteractions(service);
    }

    @Test
    void tipoDeRecursoDesconhecidoEhRecusado() throws Exception {
        String tipoInvalido = JSON_VALIDO.replace("SALA", "FERRAMENTA");

        mvc.perform(post("/api/manutencoes")
                        .with(user("admin").roles("ADMINISTRADOR")).with(csrf())
                        .contentType("application/json").content(tipoInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));

        verifyNoInteractions(service);
    }

    @Test
    void filtroDeEstadoDesconhecidoEhRecusado() throws Exception {
        mvc.perform(get("/api/manutencoes").param("status", "ARQUIVADO")
                        .with(user("leitor").roles("SOLICITANTE")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
    @Test
    void filtroPorRecursoSemTipoEhRecusado() throws Exception {
        mvc.perform(get("/api/manutencoes").param("recursoId", "12")
                        .with(user("leitor").roles("SOLICITANTE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));

        verifyNoInteractions(service);
    }

    @Test
    void filtroPorRecursoComTipoEhAceito() throws Exception {
        when(service.listar(TipoRecurso.SALA, 12L, null)).thenReturn(List.of(saida()));

        mvc.perform(get("/api/manutencoes").param("recursoTipo", "SALA").param("recursoId", "12")
                        .with(user("leitor").roles("SOLICITANTE")))
                .andExpect(status().isOk());
    }
}