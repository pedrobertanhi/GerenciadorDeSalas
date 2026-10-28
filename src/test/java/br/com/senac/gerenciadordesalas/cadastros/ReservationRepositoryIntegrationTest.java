package br.com.senac.gerenciadordesalas.cadastros;

import br.com.senac.gerenciadordesalas.material.Material;
import br.com.senac.gerenciadordesalas.material.MaterialRepository;
import br.com.senac.gerenciadordesalas.material.MaterialTipo;
import br.com.senac.gerenciadordesalas.professor.Professor;
import br.com.senac.gerenciadordesalas.professor.ProfessorRepository;
import br.com.senac.gerenciadordesalas.reservas.Reservation;
import br.com.senac.gerenciadordesalas.reservas.ReservationMaterial;
import br.com.senac.gerenciadordesalas.reservas.ReservationRepository;
import br.com.senac.gerenciadordesalas.reservas.StatusReserva;
import br.com.senac.gerenciadordesalas.salas.Sala;
import br.com.senac.gerenciadordesalas.salas.SalaRepository;
import br.com.senac.gerenciadordesalas.usuario.Role;
import br.com.senac.gerenciadordesalas.usuario.User;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Persistencia e consultas do {@link ReservationRepository} (RSV-01, #23; finalidade em RSV-02, #24).
 * Usa a base compartilhada de integracao com PostgreSQL real via Testcontainers.
 */
class ReservationRepositoryIntegrationTest extends IntegracaoPostgresBase {

    @Autowired ReservationRepository reservas;
    @Autowired UserRepository usuarios;
    @Autowired SalaRepository salas;
    @Autowired ProfessorRepository professores;
    @Autowired MaterialRepository materiais;

    private User solicitante;
    private Sala sala;
    private Professor professor;
    private Material projetor;
    private final Instant inicio = Instant.parse("2026-11-10T09:00:00Z");
    private final Instant termino = inicio.plus(2, ChronoUnit.HOURS);
    private static final String FINALIDADE = "Reserva de teste";

    @BeforeEach
    void preparar() {
        solicitante = usuarios.save(new User(
                "Ana Souza", "ana@exemplo.test", "$2a$10$hashBcryptFalso", Role.SOLICITANTE));
        sala = salas.save(new Sala("Sala 101", 30, "Bloco A - 1o andar"));
        professor = professores.save(new Professor(
                "Bruno Lima", "bruno@exemplo.test", Collections.emptySet()));
        projetor = materiais.save(new Material(
                "Projetor", "Projetor HDMI", MaterialTipo.EQUIPAMENTO, "P-0001",
                5, "Almoxarifado", false));
    }

    @Test
    void persiste_reservaMinima_comStatusInicialCriada() {
        Reservation nova = new Reservation(solicitante, sala, null, inicio, termino, FINALIDADE);
        Reservation salva = reservas.saveAndFlush(nova);
        assertThat(salva.getId()).isNotNull();
        assertThat(salva.getStatus()).isEqualTo(StatusReserva.CRIADA);
        assertThat(salva.getVersao()).isZero();
        assertThat(salva.getCriadoEm()).isNotNull();
        assertThat(salva.getAtualizadoEm()).isNotNull();
        assertThat(salva.getProfessor()).isNull();
        assertThat(salva.getFinalidade()).isEqualTo(FINALIDADE);
    }

    @Test
    void persiste_reservaComProfessorEMateriais() {
        Reservation nova = new Reservation(solicitante, sala, professor, inicio, termino, FINALIDADE);
        nova.adicionarMaterial(projetor, 2);
        Reservation salva = reservas.saveAndFlush(nova);
        entityManager.clear();
        Reservation recarregada = reservas.findById(salva.getId()).orElseThrow();
        assertThat(recarregada.getProfessor().getId()).isEqualTo(professor.getId());
        assertThat(recarregada.getMateriais()).hasSize(1);
        ReservationMaterial item = recarregada.getMateriais().get(0);
        assertThat(item.getMaterial().getId()).isEqualTo(projetor.getId());
        assertThat(item.getQuantidade()).isEqualTo(2);
    }

    @Test
    void transicaoDeStatus_incrementaVersao() {
        Reservation nova = reservas.saveAndFlush(
                new Reservation(solicitante, sala, null, inicio, termino, FINALIDADE));
        nova.transicionarPara(StatusReserva.APROVADA);
        Reservation atualizada = reservas.saveAndFlush(nova);
        assertThat(atualizada.getStatus()).isEqualTo(StatusReserva.APROVADA);
        assertThat(atualizada.getVersao()).isPositive();
    }

    @Test
    void removerMaterialDaReserva_apagaItemPorOrphanRemoval() {
        Reservation nova = new Reservation(solicitante, sala, null, inicio, termino, FINALIDADE);
        ReservationMaterial item = nova.adicionarMaterial(projetor, 1);
        reservas.saveAndFlush(nova);
        nova.removerMaterial(item);
        reservas.saveAndFlush(nova);
        entityManager.clear();
        Reservation recarregada = reservas.findById(nova.getId()).orElseThrow();
        assertThat(recarregada.getMateriais()).isEmpty();
    }

    @Test
    void alteracaoDeMateriais_atualizaAtualizadoEm() throws InterruptedException {
        Reservation nova = reservas.saveAndFlush(
                new Reservation(solicitante, sala, null, inicio, termino, FINALIDADE));
        Instant antes = nova.getAtualizadoEm();
        Thread.sleep(5);
        nova.adicionarMaterial(projetor, 1);
        Reservation apos = reservas.saveAndFlush(nova);
        assertThat(apos.getAtualizadoEm()).isAfter(antes);
    }

    @Test
    void buscarAtivasDaSalaSobrepondo_retornaIntersecao() {
        reservas.saveAndFlush(new Reservation(solicitante, sala, null, inicio, termino, FINALIDADE));

        List<Reservation> conflita = reservas.buscarAtivasDaSalaSobrepondo(
                sala.getId(),
                inicio.plus(30, ChronoUnit.MINUTES),
                termino.plus(30, ChronoUnit.MINUTES));
        assertThat(conflita).hasSize(1);

        List<Reservation> antes = reservas.buscarAtivasDaSalaSobrepondo(
                sala.getId(),
                inicio.minus(2, ChronoUnit.HOURS),
                inicio);
        assertThat(antes).isEmpty();

        List<Reservation> depois = reservas.buscarAtivasDaSalaSobrepondo(
                sala.getId(),
                termino,
                termino.plus(2, ChronoUnit.HOURS));
        assertThat(depois).isEmpty();
    }

    @Test
    void buscarAtivasDaSalaSobrepondo_ignoraOutraSalaMesmoPeriodo() {
        Sala outraSala = salas.save(new Sala("Sala 202", 40, "Bloco B"));
        reservas.saveAndFlush(new Reservation(solicitante, outraSala, null, inicio, termino, FINALIDADE));
        List<Reservation> achadas = reservas.buscarAtivasDaSalaSobrepondo(
                sala.getId(), inicio, termino);
        assertThat(achadas).isEmpty();
    }

    @Test
    void buscarAtivasDaSalaSobrepondo_ignoraReservaCancelada() {
        Reservation nova = reservas.saveAndFlush(
                new Reservation(solicitante, sala, null, inicio, termino, FINALIDADE));
        nova.transicionarPara(StatusReserva.CANCELADA);
        reservas.saveAndFlush(nova);
        List<Reservation> achadas = reservas.buscarAtivasDaSalaSobrepondo(
                sala.getId(), inicio, termino);
        assertThat(achadas).isEmpty();
    }

    @Test
    void buscarAtivasDaSalaSobrepondo_ignoraReservaNaoCompareceu() {
        Reservation nova = reservas.saveAndFlush(
                new Reservation(solicitante, sala, null, inicio, termino, FINALIDADE));
        nova.transicionarPara(StatusReserva.APROVADA);
        reservas.saveAndFlush(nova);
        nova.transicionarPara(StatusReserva.NAO_COMPARECEU);
        reservas.saveAndFlush(nova);
        List<Reservation> achadas = reservas.buscarAtivasDaSalaSobrepondo(
                sala.getId(), inicio, termino);
        assertThat(achadas).isEmpty();
    }

    @Test
    void buscarAtivasDoProfessorSobrepondo_detectaConflito() {
        reservas.saveAndFlush(new Reservation(solicitante, sala, professor, inicio, termino, FINALIDADE));
        List<Reservation> achadas = reservas.buscarAtivasDoProfessorSobrepondo(
                professor.getId(),
                inicio.plus(1, ChronoUnit.HOURS),
                termino.plus(1, ChronoUnit.HOURS));
        assertThat(achadas).hasSize(1);
    }

    @Test
    void buscarAtivasDoProfessorSobrepondo_ignoraOutroProfessorMesmoPeriodo() {
        Professor outroProfessor = professores.save(new Professor(
                "Carla Souza", "carla@exemplo.test", Collections.emptySet()));
        reservas.saveAndFlush(new Reservation(solicitante, sala, outroProfessor, inicio, termino, FINALIDADE));
        List<Reservation> achadas = reservas.buscarAtivasDoProfessorSobrepondo(
                professor.getId(), inicio, termino);
        assertThat(achadas).isEmpty();
    }

    @Test
    void findBySolicitanteId_retornaReservasEmOrdemDescendentePorInicio() {
        reservas.saveAndFlush(new Reservation(solicitante, sala, null, inicio, termino, FINALIDADE));
        reservas.saveAndFlush(new Reservation(solicitante, sala, null,
                inicio.plus(1, ChronoUnit.DAYS), termino.plus(1, ChronoUnit.DAYS), FINALIDADE));
        List<Reservation> achadas = reservas.findBySolicitanteIdOrderByInicioDesc(solicitante.getId());
        assertThat(achadas).hasSize(2);
        assertThat(achadas.get(0).getInicio()).isAfter(achadas.get(1).getInicio());
    }

    @Test
    void findBySolicitanteId_ignoraReservasDeOutroSolicitante() {
        User outroSolicitante = usuarios.save(new User(
                "Diego Alves", "diego@exemplo.test", "$2a$10$hashBcryptFalso", Role.SOLICITANTE));
        reservas.saveAndFlush(new Reservation(outroSolicitante, sala, null, inicio, termino, FINALIDADE));
        List<Reservation> achadas = reservas.findBySolicitanteIdOrderByInicioDesc(solicitante.getId());
        assertThat(achadas).isEmpty();
    }
}