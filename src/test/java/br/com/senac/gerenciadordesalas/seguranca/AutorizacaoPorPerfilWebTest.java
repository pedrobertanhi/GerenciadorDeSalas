package br.com.senac.gerenciadordesalas.seguranca;

import br.com.senac.gerenciadordesalas.erro.CorrelationIdFilter;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AutorizacaoTestController.class)
@Import({SecurityConfig.class, RespostasDeSeguranca.class, UsuarioDetailsService.class,
        AutorizacaoDePropriedade.class, CorrelationIdFilter.class})
class AutorizacaoPorPerfilWebTest {

    private static final String EMAIL_SOLICITANTE = "solicitante@exemplo.test";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AutorizacaoTestController controller;

    @MockitoBean
    UserRepository userRepository;

    @Test
    void rotaProtegidaSemAutenticacao_retorna401Padronizado() throws Exception {
        mockMvc.perform(get("/api/disponibilidade"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    @WithMockUser(username = EMAIL_SOLICITANTE, roles = "SOLICITANTE")
    void solicitante_criaReservaEConsultaSomenteAPropria() throws Exception {
        mockMvc.perform(post("/api/reservas").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("reserva-criada"));

        mockMvc.perform(get("/api/reservas/minhas/{emailDoDono}", EMAIL_SOLICITANTE))
                .andExpect(status().isOk())
                .andExpect(content().string("reserva-propria"));

        mockMvc.perform(get("/api/reservas/minhas/{emailDoDono}", "outra.pessoa@exemplo.test"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = EMAIL_SOLICITANTE, roles = "SOLICITANTE")
    void solicitante_naoAprovaNemExecutaAcaoAdministrativaSemEfeitoColateral() throws Exception {
        int alteracoesAntes = controller.totalDeAlteracoesAdministrativas();

        mockMvc.perform(post("/api/reservas/1/aprovar").with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));

        mockMvc.perform(post("/api/admin/usuarios").with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));

        assertThat(controller.totalDeAlteracoesAdministrativas()).isEqualTo(alteracoesAntes);
    }

    @Test
    @WithMockUser(username = "responsavel@exemplo.test", roles = "RESPONSAVEL")
    void responsavel_aprovaERegistraMovimentacaoMasNaoCriaReserva() throws Exception {
        mockMvc.perform(post("/api/reservas/1/aprovar").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("reserva-aprovada-1"));

        mockMvc.perform(post("/api/movimentacoes/retirada").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("retirada-registrada"));

        mockMvc.perform(post("/api/reservas").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@exemplo.test", roles = "ADMINISTRADOR")
    void administrador_gerenciaUsuariosSemHerdarOperacoesDoResponsavel() throws Exception {
        int alteracoesAntes = controller.totalDeAlteracoesAdministrativas();

        mockMvc.perform(post("/api/admin/usuarios").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("usuario-criado"));

        assertThat(controller.totalDeAlteracoesAdministrativas()).isEqualTo(alteracoesAntes + 1);

        mockMvc.perform(post("/api/reservas/1/aprovar").with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/movimentacoes/retirada").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@exemplo.test", roles = "ADMINISTRADOR")
    void exclusaoFisicaDeHistoricoPermaneceNegadaAQualquerPerfil() throws Exception {
        mockMvc.perform(delete("/api/usuarios/1").with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));

        mockMvc.perform(delete("/api/reservas/1").with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }
}
