package br.com.senac.gerenciadordesalas.reservas;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import br.com.senac.gerenciadordesalas.erro.RecursoNaoEncontradoException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Ponto unico para aplicar transicoes no ciclo de vida de reservas (RSV-04, #26).
 * As regras de transicao permanecem encapsuladas em {@link Reservation};
 * este servico carrega a reserva e persiste somente uma transicao validada.
 */
@Service
@ConditionalOnBean(ReservationRepository.class)
public class ReservationStatusService {

    private final ReservationRepository reservas;

    public ReservationStatusService(ReservationRepository reservas) {
        this.reservas = reservas;
    }

    @Transactional
    public Reservation transicionar(Long reservaId, StatusReserva novoStatus) {
        Objects.requireNonNull(reservaId, "reserva e obrigatoria");
        Objects.requireNonNull(novoStatus, "novo status e obrigatorio");

        Reservation reserva = reservas.findById(reservaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva nao encontrada"));

        reserva.transicionarPara(novoStatus);
        try {
            return reservas.saveAndFlush(reserva);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ConflitoDeEstadoException("Reserva alterada por outra operacao");
        }
    }
}
