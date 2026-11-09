package br.com.senac.gerenciadordesalas.cadastros;

import br.com.senac.gerenciadordesalas.material.Material;
import br.com.senac.gerenciadordesalas.material.MaterialRepository;
import br.com.senac.gerenciadordesalas.material.MaterialTipo;
import br.com.senac.gerenciadordesalas.professor.Professor;
import br.com.senac.gerenciadordesalas.professor.ProfessorRepository;
import br.com.senac.gerenciadordesalas.reservas.ReservationDto;
import br.com.senac.gerenciadordesalas.reservas.ReservationManagementService;
import br.com.senac.gerenciadordesalas.reservas.ReservationService;
import br.com.senac.gerenciadordesalas.reservas.StatusReserva;
import br.com.senac.gerenciadordesalas.salas.Sala;
import br.com.senac.gerenciadordesalas.salas.SalaRepository;
import br.com.senac.gerenciadordesalas.usuario.Role;
import br.com.senac.gerenciadordesalas.usuario.User;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fila de aprovacao (APR-01, #34) com PostgreSQL real:
 * reserva comum nasce APROVADA e nao entra na fila; reserva com material restrito
 * ou com professor nasce CRIADA e aparece na fila, da solicitacao mais antiga para
 * a mais recente; somente o RESPONSAVEL acessa a fila.
 */
@Import(RelogioFixoTestConfig.class)
class AprovacaoFilaIntegrationTest extends IntegracaoPostgresBase {

    private static final String EMAIL = "solicitante.fila@exemplo.test";
    private static final String URL = "/api/aprovacoes";
    private static final String EMAIL_RESPONSAVEL = "responsavel.fila@exemplo.test";
    private static final OffsetDateTime INICIO = OffsetDateTime.of(2026, 11, 20, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime TERMINO = INICIO.plusHours(2);

    @Autowired UserRepository usuarios;
    @Autowired SalaRepository salas;
    @Autowired ProfessorRepository professores;
    @Autowired MaterialRepository materiais;
    @Autowired ReservationService reservationService;
    @Autowired ReservationManagementService managementService;

    private Long salaA;
    private Long salaB;
    private Long salaC;
    private Long professorId;
    private Long microscopioRestrito;
    private Long projetorComum;

    @BeforeEach
    void preparar() {
        usuarios.save(new User("Ana Fila", EMAIL, "$2a$10$hashBcryptFalso", Role.SOLICITANTE));
        usuarios.save(new User("Responsavel Fila", EMAIL_RESPONSAVEL, "$2a$10$hashBcryptFalso", Role.RESPONSAVEL));
        salaA = salas.save(new Sala("Sala Fila A", 30, "Bloco F")).getId();
        salaB = salas.save(new Sala("Sala Fila B", 30, "Bloco F")).getId();
        salaC = salas.save(new Sala("Sala Fila C", 30, "Bloco F")).getId();
        professorId = professores.save(new Professor(
                "Bruno Fila", "bruno.fila@exemplo.test", Set.of())).getId();
        microscopioRestrito = materiais.save(new Material(
                "Microscopio", "Microscopio de laboratorio", MaterialTipo.EQUIPAMENTO, null,
                3, "Laboratorio", true)).getId();
        projetorComum = materiais.save(new Material(
                "Projetor", "Projetor HDMI", MaterialTipo.EQUIPAMENTO, null,
                5, "Almoxarifado", false)).getId();
        sincronizar();
    }

    @Test
    void reservaComum_nasceAprovadaENaoEntraNaFila() throws Exception {
        ReservationDto.Saida comum = criar(salaA, null, projetorComum);

        assertThat(comum.status()).isEqualTo(StatusReserva.APROVADA);
        mockMvc.perform(get(URL).with(user("responsavel.fila@exemplo.test").roles("RESPONSAVEL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void responsavel_veSomenteAsPendentes_daSolicitacaoMaisAntigaParaAMaisRecente() throws Exception {
        criar(salaA, null, projetorComum);
        ReservationDto.Saida restrita = criar(salaB, null, microscopioRestrito);
        ReservationDto.Saida comProfessor = criar(salaC, professorId, null);

        assertThat(restrita.status()).isEqualTo(StatusReserva.CRIADA);
        assertThat(comProfessor.status()).isEqualTo(StatusReserva.CRIADA);

        mockMvc.perform(get(URL).with(user("responsavel.fila@exemplo.test").roles("RESPONSAVEL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].reservaId").value(restrita.id()))
                .andExpect(jsonPath("$[0].solicitanteNome").value("Ana Fila"))
                .andExpect(jsonPath("$[0].solicitanteEmail").value(EMAIL))
                .andExpect(jsonPath("$[0].salaNome").value("Sala Fila B"))
                .andExpect(jsonPath("$[0].inicio").exists())
                .andExpect(jsonPath("$[0].termino").exists())
                .andExpect(jsonPath("$[0].solicitadaEm").exists())
                .andExpect(jsonPath("$[0].aprovacaoRecurso").value("PENDENTE"))
                .andExpect(jsonPath("$[0].validacaoDocente").value("NAO_EXIGIDA"))
                .andExpect(jsonPath("$[0].materiais[0].nome").value("Microscopio"))
                .andExpect(jsonPath("$[0].materiais[0].restrito").value(true))
                .andExpect(jsonPath("$[1].reservaId").value(comProfessor.id()))
                .andExpect(jsonPath("$[1].professorNome").value("Bruno Fila"))
                .andExpect(jsonPath("$[1].aprovacaoRecurso").value("NAO_EXIGIDA"))
                .andExpect(jsonPath("$[1].validacaoDocente").value("PENDENTE"));
    }

    @Test
    void reservaCancelada_saiDaFila() throws Exception {
        ReservationDto.Saida restrita = criar(salaB, null, microscopioRestrito);

        managementService.cancelar(restrita.id(), EMAIL);

        mockMvc.perform(get(URL).with(user("responsavel.fila@exemplo.test").roles("RESPONSAVEL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void alteracaoQueRemoveOMaterialRestrito_aprovaAutomaticamenteETiraDaFila() throws Exception {
        ReservationDto.Saida restrita = criar(salaB, null, microscopioRestrito);

        ReservationDto.Saida alterada = managementService.alterar(restrita.id(),
                new ReservationDto.Alteracao(salaB, null, INICIO, TERMINO, "Sem o microscopio",
                        List.of(new ReservationDto.MaterialPedido(projetorComum, 1))), EMAIL);

        assertThat(alterada.status()).isEqualTo(StatusReserva.APROVADA);
        mockMvc.perform(get(URL).with(user("responsavel.fila@exemplo.test").roles("RESPONSAVEL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @ParameterizedTest(name = "{0} recebe 403")
    @ValueSource(strings = {"SOLICITANTE", "ADMINISTRADOR"})
    void outrosPerfis_naoAcessamAFila(String perfil) throws Exception {
        criar(salaB, null, microscopioRestrito);

        mockMvc.perform(get(URL).with(user("outro.fila@exemplo.test").roles(perfil)))
                .andExpect(status().isForbidden());
    }

    @Test
    void semAutenticacao_recebe401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
    }

    private ReservationDto.Saida criar(Long salaId, Long professor, Long materialId) {
        List<ReservationDto.MaterialPedido> pedidos = materialId == null
                ? List.of()
                : List.of(new ReservationDto.MaterialPedido(materialId, 1));
        return reservationService.criar(new ReservationDto.Entrada(
                salaId, professor, INICIO, TERMINO, "Aula da fila", pedidos), EMAIL);
    }
}
