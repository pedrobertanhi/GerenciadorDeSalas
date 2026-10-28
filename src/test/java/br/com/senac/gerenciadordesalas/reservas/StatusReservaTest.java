package br.com.senac.gerenciadordesalas.reservas;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Matriz completa da maquina de estados (RSV-04, #26).
 * Para cada par origem/destino, a expectativa deriva da tabela oficial
 * exposta por {@link StatusReserva#transicoesPermitidas()}.
 */
class StatusReservaTest {

    @ParameterizedTest
    @EnumSource(StatusReserva.class)
    void matrizCompleta_respeitaTodasAsTransicoesPermitidasERecusas(StatusReserva origem) {
        for (StatusReserva destino : StatusReserva.values()) {
            boolean esperada = origem.transicoesPermitidas().contains(destino);
            assertThat(origem.podeTransicionarPara(destino))
                    .as("transicao de %s para %s", origem, destino)
                    .isEqualTo(esperada);
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
}
