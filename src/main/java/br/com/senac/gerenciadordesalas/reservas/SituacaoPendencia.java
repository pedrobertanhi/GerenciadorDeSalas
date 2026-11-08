package br.com.senac.gerenciadordesalas.reservas;

/**
 * Situacao de cada decisao que uma reserva pode exigir do Responsavel (APR-01, #34):
 * a aprovacao de recurso restrito e a validacao docente.
 */
public enum SituacaoPendencia {

    /** A reserva nao precisa desta decisao. */
    NAO_EXIGIDA,

    /** Aguardando a decisao do Responsavel. */
    PENDENTE,

    /** O Responsavel aprovou (ou validou, no caso docente). */
    APROVADA,

    /** O Responsavel rejeitou, com motivo. */
    REJEITADA;

    /** {@code true} se esta decisao ainda impede a aprovacao da reserva. */
    public boolean bloqueiaAprovacao() {
        return this == PENDENTE;
    }
}
