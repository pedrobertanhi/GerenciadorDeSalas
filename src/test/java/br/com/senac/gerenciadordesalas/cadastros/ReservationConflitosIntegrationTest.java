package br.com.senac.gerenciadordesalas.cadastros;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suite parametrizada de conflitos de reserva (#27: RSV-06, RSV-07, RSV-08 e RSV-09).
 *
 * Uma reserva base ocupa [10:00, 12:00) e cada caso tenta outra reserva no mesmo
 * recurso. O termino e exclusivo, entao horarios adjacentes nao conflitam.
 * <ul>
 *   <li>SALA: a tentativa usa a mesma sala;</li>
 *   <li>PROFESSOR: a tentativa usa o mesmo professor em outra sala;</li>
 *   <li>MATERIAL: a tentativa pede, em outra sala, o mesmo equipamento de
 *       patrimonio unico (quantidade total 1).</li>
 * </ul>
 * Para professor e material a tentativa fica em outra sala de proposito: assim o
 * unico recurso compartilhado e o que esta sendo testado.
 */
@Import(RelogioFixoTestConfig.class)
class ReservationConflitosIntegrationTest extends IntegracaoPostgresBase {

    private static final String EMAIL = "solicitante.conflitos@exemplo.test";
    private static final OffsetDateTime DIA = OffsetDateTime.of(2026, 11, 20, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final LocalTime BASE_INICIO = LocalTime.of(10, 0);
    private static final LocalTime BASE_TERMINO = LocalTime.of(12, 0);

    /** Estados que ocupam o recurso. Definidos aqui de forma explicita, sem ler o codigo de producao. */
    private static final Set<StatusReserva> BLOQUEADORES =
            EnumSet.of(StatusReserva.CRIADA, StatusReserva.APROVADA, StatusReserva.EM_ANDAMENTO);

    private static final List<Caso> CASOS = List.of(
            new Caso("mesmo intervalo", hora(10, 0), hora(12, 0), true),
            new Caso("contido", hora(10, 30), hora(11, 30), true),
            new Caso("contem", hora(9, 0), hora(13, 0), true),
            new Caso("sobrepoe o inicio", hora(9, 0), hora(10, 30), true),
            new Caso("sobrepoe o fim", hora(11, 30), hora(13, 0), true),
            new Caso("um minuto dentro do inicio", hora(9, 0), hora(10, 1), true),
            new Caso("um minuto dentro do fim", hora(11, 59), hora(13, 0), true),
            new Caso("adjacente antes", hora(9, 0), hora(10, 0), false),
            new Caso("adjacente depois", hora(12, 0), hora(13, 0), false),
            new Caso("totalmente antes", hora(7, 0), hora(8, 0), false),
            new Caso("totalmente depois", hora(14, 0), hora(15, 0), false));

    @Autowired UserRepository usuarios;
    @Autowired SalaRepository salas;
    @Autowired ProfessorRepository professores;
    @Autowired MaterialRepository materiais;
    @Autowired ReservationService reservationService;
    @Autowired ReservationManagementService managementService;

    private Long salaA;
    private Long salaB;
    private Long professorId;
    private Long patrimonioId;

    enum Recurso {
        SALA("Sala ja possui reserva no periodo informado"),
        PROFESSOR("Professor ja possui reserva no periodo informado"),
        MATERIAL("nao possui quantidade disponivel no periodo informado");

        private final String mensagem;

        Recurso(String mensagem) {
            this.mensagem = mensagem;
        }
    }

    record Caso(String nome, LocalTime inicio, LocalTime termino, boolean conflita) {
        @Override
        public String toString() {
            return nome + " [" + inicio + ", " + termino + ") -> " + (conflita ? "conflito" : "aceito");
        }
    }

    @BeforeEach
    void preparar() {
        usuarios.save(new User("Ana Conflitos", EMAIL, "$2a$10$hashBcryptFalso", Role.SOLICITANTE));
        salaA = criarSala("Sala Conflitos A");
        salaB = criarSala("Sala Conflitos B");
        professorId = professores.save(new Professor(
                "Carlos Conflitos", "carlos.conflitos@exemplo.test", Set.of())).getId();
        patrimonioId = materiais.save(new Material(
                "Notebook", "Notebook patrimoniado", MaterialTipo.EQUIPAMENTO, "PAT-0001",
                1, "Almoxarifado", false)).getId();
        sincronizar();
    }

    // RSV-09: tabela de intervalos aplicada a sala, professor e material (RSV-06, RSV-07, RSV-08).

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("recursosECasos")
    void criacao_respeitaAFormulaDeSobreposicao(Recurso recurso, Caso caso) {
        reservaBase(recurso);

        if (caso.conflita()) {
            assertThatThrownBy(() -> tentativa(recurso, caso.inicio(), caso.termino()))
                    .isInstanceOf(ConflitoDeEstadoException.class)
                    .hasMessageContaining(recurso.mensagem);
            assertThat(contar("tb_reservas")).isEqualTo(1);
        } else {
            tentativa(recurso, caso.inicio(), caso.termino());
            assertThat(contar("tb_reservas")).isEqualTo(2);
        }
    }

    static Stream<Arguments> recursosECasos() {
        return Arrays.stream(Recurso.values())
                .flatMap(recurso -> CASOS.stream().map(caso -> arguments(recurso, caso)));
    }

    // Estados: so CRIADA, APROVADA e EM_ANDAMENTO bloqueiam; cancelada e demais finais liberam.

    @ParameterizedTest(name = "{0} | reserva base {1}")
    @MethodSource("recursosEStatus")
    void somenteEstadosAtivosBloqueiamORecurso(Recurso recurso, StatusReserva status) {
        ReservationDto.Saida base = reservaBase(recurso);
        jdbcTemplate.update("UPDATE tb_reservas SET status = ? WHERE id = ?", status.name(), base.id());
        entityManager.clear();

        if (BLOQUEADORES.contains(status)) {
            assertThatThrownBy(() -> tentativa(recurso, BASE_INICIO, BASE_TERMINO))
                    .isInstanceOf(ConflitoDeEstadoException.class)
                    .hasMessageContaining(recurso.mensagem);
            assertThat(contar("tb_reservas")).isEqualTo(1);
        } else {
            tentativa(recurso, BASE_INICIO, BASE_TERMINO);
            assertThat(contar("tb_reservas")).isEqualTo(2);
        }
    }

    static Stream<Arguments> recursosEStatus() {
        return Arrays.stream(Recurso.values())
                .flatMap(recurso -> Arrays.stream(StatusReserva.values())
                        .map(status -> arguments(recurso, status)));
    }

    // Alteracao: a reserva nao conflita consigo mesma, mas nao pode passar a sobrepor outra.

    @ParameterizedTest(name = "{0}")
    @EnumSource(Recurso.class)
    void alteracao_dentroDoProprioHorario_naoConflitaConsigoMesma(Recurso recurso) {
        ReservationDto.Saida base = reservaBase(recurso);

        ReservationDto.Saida alterada = managementService.alterar(base.id(),
                alteracao(salaA, recurso, hora(10, 30), hora(11, 30)), EMAIL);

        assertThat(alterada.inicio()).isEqualTo(DIA.with(hora(10, 30)).toInstant());
        assertThat(contar("tb_reservas")).isEqualTo(1);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Recurso.class)
    void alteracao_queSobrepoeOutraReserva_ehRecusada(Recurso recurso) {
        reservaBase(recurso);
        ReservationDto.Saida outra = tentativa(recurso, hora(14, 0), hora(15, 0));
        Long salaDaOutra = recurso == Recurso.SALA ? salaA : salaB;

        assertThatThrownBy(() -> managementService.alterar(outra.id(),
                alteracao(salaDaOutra, recurso, hora(11, 0), hora(13, 0)), EMAIL))
                .isInstanceOf(ConflitoDeEstadoException.class)
                .hasMessageContaining(recurso.mensagem);
    }

    // API: o conflito chega ao cliente como 409 com codigo padronizado.

    @ParameterizedTest(name = "{0}")
    @EnumSource(Recurso.class)
    void api_conflito_recebe409(Recurso recurso) throws Exception {
        reservaBase(recurso);
        Long sala = recurso == Recurso.SALA ? salaA : salaB;
        String professor = recurso == Recurso.PROFESSOR ? "\"professorId\": " + professorId + "," : "";
        String material = recurso == Recurso.MATERIAL
                ? "{\"materialId\": " + patrimonioId + ", \"quantidade\": 1}" : "";
        String corpo = """
                {
                  "salaId": %d,
                  %s
                  "inicio": "%s",
                  "termino": "%s",
                  "finalidade": "Aula de teste",
                  "materiais": [%s]
                }
                """.formatted(sala, professor, DIA.with(hora(11, 0)), DIA.with(hora(13, 0)), material);

        mockMvc.perform(post("/api/reservas")
                        .with(user(EMAIL).roles("SOLICITANTE"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_DE_ESTADO"));

        assertThat(contar("tb_reservas")).isEqualTo(1);
    }

    // RSV-08: soma das quantidades simultaneas contra o total do material (total 5).

    @ParameterizedTest(name = "ja reservadas {0} + pedido {1} de 5 -> aceito = {2}")
    @CsvSource(delimiter = '|', value = {
            "0     | 5 | true",
            "0     | 6 | false",
            "3     | 2 | true",
            "3     | 3 | false",
            "5     | 1 | false",
            "2;2   | 1 | true",
            "2;2   | 2 | false",
            "1;1;1 | 2 | true"
    })
    void quantidadeReservadaNuncaSuperaOTotal(String jaReservadas, int pedido, boolean aceito) {
        Long projetor = materiais.save(new Material(
                "Projetor", "Projetor HDMI", MaterialTipo.EQUIPAMENTO, null,
                5, "Almoxarifado", false)).getId();
        int salaSeq = 0;
        if (!"0".equals(jaReservadas)) {
            for (String quantidade : jaReservadas.split(";")) {
                criarComMaterial(criarSala("Sala Qtd " + salaSeq++), projetor, Integer.parseInt(quantidade));
            }
        }
        int antes = contar("tb_reservas");
        Long salaDoPedido = criarSala("Sala Qtd Pedido");

        if (aceito) {
            criarComMaterial(salaDoPedido, projetor, pedido);
            assertThat(contar("tb_reservas")).isEqualTo(antes + 1);
        } else {
            assertThatThrownBy(() -> criarComMaterial(salaDoPedido, projetor, pedido))
                    .isInstanceOf(ConflitoDeEstadoException.class);
            assertThat(contar("tb_reservas")).isEqualTo(antes);
        }
    }

    // Auxiliares

    private static LocalTime hora(int hora, int minuto) {
        return LocalTime.of(hora, minuto);
    }

    private Long criarSala(String nome) {
        return salas.save(new Sala(nome, 30, "Bloco de testes")).getId();
    }

    private ReservationDto.Saida reservaBase(Recurso recurso) {
        return reservationService.criar(entrada(salaA, recurso, BASE_INICIO, BASE_TERMINO), EMAIL);
    }

    private ReservationDto.Saida tentativa(Recurso recurso, LocalTime inicio, LocalTime termino) {
        Long sala = recurso == Recurso.SALA ? salaA : salaB;
        return reservationService.criar(entrada(sala, recurso, inicio, termino), EMAIL);
    }

    private ReservationDto.Saida criarComMaterial(Long salaId, Long materialId, int quantidade) {
        return reservationService.criar(new ReservationDto.Entrada(
                salaId, null, DIA.with(BASE_INICIO), DIA.with(BASE_TERMINO), "Aula de teste",
                List.of(new ReservationDto.MaterialPedido(materialId, quantidade))), EMAIL);
    }

    private ReservationDto.Entrada entrada(Long salaId, Recurso recurso, LocalTime inicio, LocalTime termino) {
        return new ReservationDto.Entrada(salaId, professorDo(recurso), DIA.with(inicio), DIA.with(termino),
                "Aula de teste", materiaisDo(recurso));
    }

    private ReservationDto.Alteracao alteracao(Long salaId, Recurso recurso, LocalTime inicio, LocalTime termino) {
        return new ReservationDto.Alteracao(salaId, professorDo(recurso), DIA.with(inicio), DIA.with(termino),
                "Aula de teste alterada", materiaisDo(recurso));
    }

    private Long professorDo(Recurso recurso) {
        return recurso == Recurso.PROFESSOR ? professorId : null;
    }

    private List<ReservationDto.MaterialPedido> materiaisDo(Recurso recurso) {
        return recurso == Recurso.MATERIAL
                ? List.of(new ReservationDto.MaterialPedido(patrimonioId, 1))
                : List.of();
    }
}
