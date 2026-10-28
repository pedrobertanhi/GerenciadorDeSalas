package br.com.senac.gerenciadordesalas.reservas;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Matriz completa da maquina de estados (RSV-04, #26).
 * Exercita todos os pares origem/destino contra a regra de negocio esperada.
 */
class StatusReservaTest {

    @ParameterizedTest
    @EnumSource(StatusReserva.class)
    void matrizCompleta_respeitaTodasAsTransicoesPermitidasERecusas(StatusReserva origem) {
        for (StatusReserva destino : StatusReserva.values()) {
            assertThat(origem.podeTransicionarPara(destino))
                    .as("transicao de %s para %s", origem, destino)
                    .isEqualTo(transicaoEsperada(origem, destino));
        }
    }

    @ParameterizedTest
    @EnumSource(value = StatusReserva.class,
            names = {"REJEITADA", "CANCELADA", "CONCLUIDA", "NAO_COMPARECEU"})
    void estadoFinal_naoPossuiTransicoes(StatusReserva estadoFinal) {
        assertThat(estadoFinal.isFinal()).isTrue();
        assertThat(estadoFinal.transicoesPermitidas()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = StatusReserva.class,
            names = {"CRIADA", "APROVADA", "EM_ANDAMENTO"})
    void estadoNaoFinal_expoeApenasSeusProximosEstados(StatusReserva estado) {
        assertThat(estado.isFinal()).isFalse();
        assertThat(estado.transicoesPermitidas()).doesNotContain(estado);
        assertThat(estado.podeTransicionarPara(null)).isFalse();
    }

    private boolean transicaoEsperada(StatusReserva origem, StatusReserva destino) {
        return switch (origem) {
            case CRIADA -> destino == StatusReserva.APROVADA
                    || destino == StatusReserva.REJEITADA
                    || destino == StatusReserva.CANCELADA;
            case APROVADA -> destino == StatusReserva.EM_ANDAMENTO
                    || destino == StatusReserva.CANCELADA
                    || destino == StatusReserva.NAO_COMPARECEU;
            case EM_ANDAMENTO -> destino == StatusReserva.CONCLUIDA;
            case REJEITADA, CANCELADA, CONCLUIDA, NAO_COMPARECEU -> false;
        };
    }
}
