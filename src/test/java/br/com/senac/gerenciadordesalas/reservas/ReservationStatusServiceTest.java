package br.com.senac.gerenciadordesalas.reservas;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import br.com.senac.gerenciadordesalas.erro.RecursoNaoEncontradoException;
import br.com.senac.gerenciadordesalas.salas.Sala;
import br.com.senac.gerenciadordesalas.usuario.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes do ponto unico de transicao de estados (RSV-04, #26).
 */
class ReservationStatusServiceTest {

    private final ReservationRepository reservas = Mockito.mock(ReservationRepository.class);
    private final ReservationStatusService service = new ReservationStatusService(reservas);
    private Reservation reserva;

    @BeforeEach
    void preparar() {
        User solicitante = Mockito.mock(User.class);
        Sala sala = Mockito.mock(Sala.class);
        Instant inicio = Instant.parse("2026-11-10T09:00:00Z");
        reserva = new Reservation(
                solicitante, sala, null, inicio, inicio.plus(2, ChronoUnit.HOURS), "Aula de teste");

        when(reservas.findById(10L)).thenReturn(Optional.of(reserva));
        when(reservas.saveAndFlush(reserva)).thenReturn(reserva);
    }

    @Test
    void transicionar_aplicaTransicaoValidaEPersiste() {
        Reservation resultado = service.transicionar(10L, StatusReserva.APROVADA);

        assertThat(resultado.getStatus()).isEqualTo(StatusReserva.APROVADA);
        verify(reservas).saveAndFlush(reserva);
    }

    @Test
    void transicionar_recusaMudancaInvalidaComErroDeNegocio() {
        assertThatThrownBy(() -> service.transicionar(10L, StatusReserva.CONCLUIDA))
                .isInstanceOf(ConflitoDeEstadoException.class)
                .hasMessage("Transicao invalida: CRIADA -> CONCLUIDA");

        verify(reservas, never()).saveAndFlush(reserva);
    }

    @Test
    void transicionar_traduzConflitoDeConcorrenciaParaErroDeNegocio() {
        when(reservas.saveAndFlush(reserva))
                .thenThrow(new ObjectOptimisticLockingFailureException(Reservation.class, 10L));

        assertThatThrownBy(() -> service.transicionar(10L, StatusReserva.APROVADA))
                .isInstanceOf(ConflitoDeEstadoException.class)
                .hasMessage("Reserva alterada por outra operacao");
    }

    @Test
    void transicionar_recusaReservaInexistente() {
        when(reservas.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transicionar(99L, StatusReserva.APROVADA))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Reserva nao encontrada");
    }
}
