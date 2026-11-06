package br.com.senac.gerenciadordesalas.reservas;

import br.com.senac.gerenciadordesalas.manutencao.TipoRecurso;
import br.com.senac.gerenciadordesalas.material.MaterialRepository;
import br.com.senac.gerenciadordesalas.professor.ProfessorRepository;
import br.com.senac.gerenciadordesalas.salas.SalaRepository;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Impacto de um bloqueio de manutenção sobre as reservas ativas do recurso (tela de manutenção,
 * #15): só conta, nunca altera, como manda a política da MNT-02.
 */
class ReservationImpactoManutencaoTest {

    private static final Instant INICIO = Instant.parse("2099-01-10T11:00:00Z");
    private static final Instant FIM = Instant.parse("2099-01-10T21:00:00Z");

    private final ReservationRepository reservas = mock(ReservationRepository.class);
    private final UserRepository usuarios = mock(UserRepository.class);
    private final ReservationService service = new ReservationService(
            reservas, usuarios, mock(SalaRepository.class), mock(ProfessorRepository.class),
            mock(MaterialRepository.class),
            new ReservationTimePolicy(Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"), ZoneOffset.UTC)));

    @Test
    void contaAsReservasAtivasDaSalaNoPeriodo() {
        when(reservas.buscarAtivasDaSalaSobrepondo(12L, INICIO, FIM))
                .thenReturn(List.of(mock(Reservation.class), mock(Reservation.class)));

        assertThat(service.contarReservasAtivasNoPeriodo(TipoRecurso.SALA, 12L, INICIO, FIM)).isEqualTo(2);
    }

    @Test
    void contaAsReservasAtivasDoProfessorNoPeriodo() {
        when(reservas.buscarAtivasDoProfessorSobrepondo(7L, INICIO, FIM))
                .thenReturn(List.of(mock(Reservation.class)));

        assertThat(service.contarReservasAtivasNoPeriodo(TipoRecurso.PROFESSOR, 7L, INICIO, FIM)).isEqualTo(1);
    }

    @Test
    void contaOsItensAtivosDoMaterialNoPeriodo() {
        when(reservas.materiaisAtivosSobrepondo(3L, INICIO, FIM))
                .thenReturn(List.of(mock(ReservationMaterial.class), mock(ReservationMaterial.class),
                        mock(ReservationMaterial.class)));

        assertThat(service.contarReservasAtivasNoPeriodo(TipoRecurso.MATERIAL, 3L, INICIO, FIM)).isEqualTo(3);
    }

    @ParameterizedTest
    @EnumSource(TipoRecurso.class)
    void semReservasNoPeriodoOResultadoEZero(TipoRecurso tipo) {
        assertThat(service.contarReservasAtivasNoPeriodo(tipo, 99L, INICIO, FIM)).isZero();
    }

    @Test
    void aConsultaNaoCriaNemAlteraNenhumaReserva() {
        service.contarReservasAtivasNoPeriodo(TipoRecurso.SALA, 12L, INICIO, FIM);

        org.mockito.Mockito.verify(reservas, org.mockito.Mockito.never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(usuarios);
    }
}