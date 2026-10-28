package br.com.senac.gerenciadordesalas.reservas;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Estados do ciclo de vida de uma reserva (RSV-01, #23; matriz em RSV-04, #26).
 * A transicao e controlada pelo dominio: nenhum setter de status
 * deve ser exposto; use {@link Reservation#transicionarPara(StatusReserva)}.
 */
public enum StatusReserva {

    CRIADA,
    APROVADA,
    REJEITADA,
    CANCELADA,
    EM_ANDAMENTO,
    CONCLUIDA,
    NAO_COMPARECEU;

    private static final Set<StatusReserva> FINAIS =
            EnumSet.of(REJEITADA, CANCELADA, CONCLUIDA, NAO_COMPARECEU);

    private static final Map<StatusReserva, Set<StatusReserva>> TRANSICOES_PERMITIDAS = Map.of(
            CRIADA, EnumSet.of(APROVADA, REJEITADA, CANCELADA),
            APROVADA, EnumSet.of(EM_ANDAMENTO, CANCELADA, NAO_COMPARECEU),
            EM_ANDAMENTO, EnumSet.of(CONCLUIDA)
    );

    /**
     * Retorna as transicoes que este status permite. A colecao e imutavel
     * para que a matriz de regras nao possa ser alterada externamente.
     */
    public Set<StatusReserva> transicoesPermitidas() {
        return TRANSICOES_PERMITIDAS.getOrDefault(this, Set.of());
    }

    /**
     * {@code true} se este status aceita a transicao para {@code novo}.
     * Status finais nao aceitam qualquer transicao e uma reserva nao
     * transiciona para o proprio estado.
     */
    public boolean podeTransicionarPara(StatusReserva novo) {
        return novo != null && novo != this && transicoesPermitidas().contains(novo);
    }

    public boolean isFinal() {
        return FINAIS.contains(this);
    }
}
