package br.com.senac.gerenciadordesalas.cadastros;

import br.com.senac.gerenciadordesalas.cadastros.NavegadorE2E.Tela;
import br.com.senac.gerenciadordesalas.material.Material;
import br.com.senac.gerenciadordesalas.material.MaterialRepository;
import br.com.senac.gerenciadordesalas.material.MaterialTipo;
import br.com.senac.gerenciadordesalas.professor.Professor;
import br.com.senac.gerenciadordesalas.professor.ProfessorRepository;
import br.com.senac.gerenciadordesalas.reservas.ReservationDto;
import br.com.senac.gerenciadordesalas.reservas.ReservationRepository;
import br.com.senac.gerenciadordesalas.reservas.ReservationService;
import br.com.senac.gerenciadordesalas.reservas.StatusReserva;
import br.com.senac.gerenciadordesalas.salas.Sala;
import br.com.senac.gerenciadordesalas.salas.SalaRepository;
import br.com.senac.gerenciadordesalas.usuario.Role;
import br.com.senac.gerenciadordesalas.usuario.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Roteiro E2E da tela de aprovacoes (UI-11, #34) em navegador real: o Responsavel rejeita
 * com motivo, aprova recurso restrito e valida docente; outros perfis nao acessam; a tela
 * funciona no celular, no tablet e no desktop.
 */
class AprovacoesNavegadorTest extends NavegadorBase {

    static final String RESPONSAVEL_EMAIL = "responsavel.e2e@exemplo.test";
    private static final OffsetDateTime INICIO = OffsetDateTime.of(2099, 5, 10, 11, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime TERMINO = INICIO.plusHours(2);

    @Autowired SalaRepository salas;
    @Autowired ProfessorRepository professores;
    @Autowired MaterialRepository materiais;
    @Autowired ReservationService reservationService;
    @Autowired ReservationRepository reservas;

    private Long salaA;
    private Long salaB;
    private Long salaC;
    private Long professorId;
    private Long microscopio;

    @BeforeEach
    void prepararAprovacoes() {
        usuarios.save(new User("Responsavel E2E", RESPONSAVEL_EMAIL, codificador.encode(SENHA), Role.RESPONSAVEL));
        salaA = salas.save(new Sala("Sala E2E A", 30, "Bloco E")).getId();
        salaB = salas.save(new Sala("Sala E2E B", 30, "Bloco E")).getId();
        salaC = salas.save(new Sala("Sala E2E C", 30, "Bloco E")).getId();
        professorId = professores.save(new Professor("Paula E2E", "paula.e2e@exemplo.test", Set.of())).getId();
        microscopio = materiais.save(new Material("Microscopio", "Microscopio de laboratorio",
                MaterialTipo.EQUIPAMENTO, null, 2, "Laboratorio", true)).getId();
    }

    @Test
    void responsavelRejeitaAprovaEValidaPelaTela() {
        Long rejeitada = criar(salaA, null, microscopio);
        Long aprovada = criar(salaB, null, microscopio);
        Long validada = criar(salaC, professorId, null);
        navegador.entrarComo(RESPONSAVEL_EMAIL, SENHA);

        navegador.ir("/aprovacoes");
        navegador.esperarTexto("Aprovações pendentes");
        navegador.clicarNaLinha("Sala E2E A", "Decidir");
        navegador.esperarTexto("Aprovação do recurso restrito");
        assertThat(navegador.textoDaPagina()).contains("Microscopio (1), restrito");
        navegador.preencher("motivoRecurso", "Microscopio reservado para pesquisa");
        navegador.clicarBotao("Rejeitar recurso");
        navegador.esperarTexto("Reserva rejeitada e motivo registrado.");

        navegador.clicarNaLinha("Sala E2E B", "Decidir");
        navegador.esperarTexto("Aprovação do recurso restrito");
        navegador.clicarBotao("Aprovar recurso");
        navegador.esperarTexto("Recurso restrito aprovado.");

        navegador.clicarNaLinha("Sala E2E C", "Decidir");
        // "Validação docente" tambem aparece na fila; "Decidir reserva" so existe na tela de decisao.
        navegador.esperarTexto("Decidir reserva");
        assertThat(navegador.textoDaPagina()).contains("Paula E2E");
        navegador.clicarBotao("Validar docente");
        navegador.esperarTexto("Alocação docente validada.");
        navegador.esperarTexto("Nenhuma reserva aguardando decisão.");

        assertThat(statusDe(rejeitada)).isEqualTo(StatusReserva.REJEITADA);
        assertThat(statusDe(aprovada)).isEqualTo(StatusReserva.APROVADA);
        assertThat(statusDe(validada)).isEqualTo(StatusReserva.APROVADA);
    }

    @Test
    void solicitanteNaoAcessaATelaDeAprovacoes() {
        criar(salaA, null, microscopio);
        navegador.entrarComo(SOLICITANTE_EMAIL, SENHA);

        navegador.ir("/aprovacoes");
        navegador.esperarTexto("ACESSO_NEGADO");
        assertThat(navegador.textoDaPagina()).doesNotContain("Sala E2E A").doesNotContain("Decidir");
    }

    @ParameterizedTest
    @EnumSource(Tela.class)
    void telaDeAprovacoesSeAdaptaAoTamanhoDaJanela(Tela tela) {
        Long id = criar(salaA, professorId, microscopio);
        navegador.usarTela(tela);
        navegador.entrarComo(RESPONSAVEL_EMAIL, SENHA);

        for (String caminho : List.of("/aprovacoes", "/aprovacoes/" + id)) {
            navegador.ir(caminho);
            assertThat(navegador.bootstrapCarregado())
                    .as("Bootstrap aplicado em %s (%s)", caminho, tela).isTrue();
            assertThat(navegador.larguraDoConteudo())
                    .as("a página %s não pode rolar na horizontal em %s", caminho, tela)
                    .isLessThanOrEqualTo(navegador.larguraDaJanela());
        }

        navegador.ir("/aprovacoes");
        assertThat(navegador.rolagemHorizontalDe(".table-responsive")).isEqualTo("auto");

        navegador.ir("/aprovacoes/" + id);
        for (String campo : List.of("motivoRecurso", "motivoDocente")) {
            assertThat(navegador.campoDentroDaJanela(campo)).as("campo %s dentro da janela em %s", campo, tela).isTrue();
        }
        for (String botao : List.of("Aprovar recurso", "Rejeitar recurso", "Validar docente", "Rejeitar docente")) {
            assertThat(navegador.botaoDentroDaJanela(botao)).as("botão %s dentro da janela em %s", botao, tela).isTrue();
        }
    }

    private Long criar(Long salaId, Long professor, Long material) {
        List<ReservationDto.MaterialPedido> pedidos = material == null
                ? List.of()
                : List.of(new ReservationDto.MaterialPedido(material, 1));
        return reservationService.criar(new ReservationDto.Entrada(
                salaId, professor, INICIO, TERMINO, "Aula E2E", pedidos), SOLICITANTE_EMAIL).id();
    }

    private StatusReserva statusDe(Long id) {
        return reservas.findById(id).orElseThrow().getStatus();
    }
}
