package br.com.senac.gerenciadordesalas.reservas;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Regras temporais de reserva (RSV-05, #27).
 * Todos os instantes sao comparados em UTC e o fim e exclusivo:
 * [inicio, termino). Assim, uma reserva que termina exatamente quando outra
 * comeca nao ocupa o mesmo intervalo.
 */
@Component
public class ReservationTimePolicy {

    private final Clock clock;

    public ReservationTimePolicy(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock e obrigatorio");
    }

    public void validarParaReserva(Instant inicio, Instant termino) {
        if (inicio == null || termino == null) {
            throw new IllegalArgumentException("Inicio e termino sao obrigatorios");
        }
        if (!termino.isAfter(inicio)) {
            throw new IllegalArgumentException("O termino deve ser posterior ao inicio");
        }
        if (inicio.isBefore(clock.instant())) {
            throw new ConflitoDeEstadoException("Reserva nao pode iniciar no passado");
        }
    }

    public boolean sobrepoe(Instant inicioA, Instant terminoA, Instant inicioB, Instant terminoB) {
        validarIntervalo(inicioA, terminoA);
        validarIntervalo(inicioB, terminoB);
        return inicioA.isBefore(terminoB) && terminoA.isAfter(inicioB);
    }

    private void validarIntervalo(Instant inicio, Instant termino) {
        if (inicio == null || termino == null || !termino.isAfter(inicio)) {
            throw new IllegalArgumentException("Intervalo de reserva invalido");
        }
    }
}
