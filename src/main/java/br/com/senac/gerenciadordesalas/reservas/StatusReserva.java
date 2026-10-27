package br.com.senac.gerenciadordesalas.reservas;

import java.util.EnumSet;
import java.util.Set;
 
/**
 * Estados do ciclo de vida de uma reserva (RSV-01, #23).
 * A transicao e controlada pelo dominio: nenhum setter de status
 * deve ser exposto; use {@link Reservation#transicionarPara(StatusReserva)}.
 */
public enum StatusReserva {

    CRIADA,
    APROVADA,
    REJEITADA,
    CANCELADA,
    EM_ANDAMENTO,
    CONCLUIDA;

    private static final Set<StatusReserva> FINAIS =
            EnumSet.of(REJEITADA, CANCELADA, CONCLUIDA);

    /**
     * {@code true} se este status aceita a transicao para {@code novo}.
     * Status finais nao aceitam qualquer transicao.
     */
    public boolean podeTransicionarPara(StatusReserva novo) {
        if (novo == null || novo == this || FINAIS.contains(this)) {
            return false;
        }
        return switch (this) {
            case CRIADA -> novo == APROVADA || novo == REJEITADA || novo == CANCELADA;
            case APROVADA -> novo == EM_ANDAMENTO || novo == CANCELADA;
            case EM_ANDAMENTO -> novo == CONCLUIDA;
            default -> false;
        };
    }

    public boolean isFinal() {
        return FINAIS.contains(this);
    }
}