package br.com.senac.gerenciadordesalas.reservas;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regras de transicao do {@link StatusReserva} (RSV-01, #23).
 * Garante que status finais nao transicionam e que o fluxo valido
 * CRIADA -> APROVADA -> EM_ANDAMENTO -> CONCLUIDA e preservado.
 */
class StatusReservaTest {

    @Test
    void criada_transicionaParaAprovadaRejeitadaOuCancelada() {
        assertThat(StatusReserva.CRIADA.podeTransicionarPara(StatusReserva.APROVADA)).isTrue();
        assertThat(StatusReserva.CRIADA.podeTransicionarPara(StatusReserva.REJEITADA)).isTrue();
        assertThat(StatusReserva.CRIADA.podeTransicionarPara(StatusReserva.CANCELADA)).isTrue();
        assertThat(StatusReserva.CRIADA.podeTransicionarPara(StatusReserva.EM_ANDAMENTO)).isFalse();
        assertThat(StatusReserva.CRIADA.podeTransicionarPara(StatusReserva.CONCLUIDA)).isFalse();
    }

    @Test
    void aprovada_transicionaParaEmAndamentoOuCancelada() {
        assertThat(StatusReserva.APROVADA.podeTransicionarPara(StatusReserva.EM_ANDAMENTO)).isTrue();
        assertThat(StatusReserva.APROVADA.podeTransicionarPara(StatusReserva.CANCELADA)).isTrue();
        assertThat(StatusReserva.APROVADA.podeTransicionarPara(StatusReserva.CRIADA)).isFalse();
        assertThat(StatusReserva.APROVADA.podeTransicionarPara(StatusReserva.REJEITADA)).isFalse();
        assertThat(StatusReserva.APROVADA.podeTransicionarPara(StatusReserva.CONCLUIDA)).isFalse();
    }

    @Test
    void emAndamento_soTransicionaParaConcluida() {
        assertThat(StatusReserva.EM_ANDAMENTO.podeTransicionarPara(StatusReserva.CONCLUIDA)).isTrue();
        assertThat(StatusReserva.EM_ANDAMENTO.podeTransicionarPara(StatusReserva.CRIADA)).isFalse();
        assertThat(StatusReserva.EM_ANDAMENTO.podeTransicionarPara(StatusReserva.APROVADA)).isFalse();
        assertThat(StatusReserva.EM_ANDAMENTO.podeTransicionarPara(StatusReserva.CANCELADA)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = StatusReserva.class, names = {"REJEITADA", "CANCELADA", "CONCLUIDA"})
    void statusFinal_naoAceitaNenhumaTransicao(StatusReserva estadoFinal) {
        for (StatusReserva alvo : StatusReserva.values()) {
            assertThat(estadoFinal.podeTransicionarPara(alvo))
                    .as("transicao de %s para %s", estadoFinal, alvo)
                    .isFalse();
        }
        assertThat(estadoFinal.isFinal()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(StatusReserva.class)
    void nenhumStatusTransicionaParaOProprioOuParaNulo(StatusReserva atual) {
        assertThat(atual.podeTransicionarPara(atual)).isFalse();
        assertThat(atual.podeTransicionarPara(null)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = StatusReserva.class, names = {"CRIADA", "APROVADA", "EM_ANDAMENTO"})
    void statusNaoFinal_naoEhFinal(StatusReserva atual) {
        assertThat(atual.isFinal()).isFalse();
    }
}
