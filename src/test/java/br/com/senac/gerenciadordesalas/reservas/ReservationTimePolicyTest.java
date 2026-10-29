package br.com.senac.gerenciadordesalas.reservas;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ReservationTimePolicyTest {

    private static final Instant AGORA = Instant.parse("2026-11-10T09:00:00Z");
    private final ReservationTimePolicy politica = new ReservationTimePolicy(
            Clock.fixed(AGORA, ZoneOffset.UTC));

    @ParameterizedTest
    @MethodSource("intervalosInvalidos")
    void recusaInicioOuTerminoInvalidos(Instant inicio, Instant termino) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> politica.validarParaReserva(inicio, termino));
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> intervalosInvalidos() {
        return Stream.of(
                arguments(null, AGORA.plusSeconds(3600)),
                arguments(AGORA, null),
                arguments(AGORA, AGORA),
                arguments(AGORA.plusSeconds(3600), AGORA)
        );
    }

    @Test
    void recusaReservaNoPassadoComClockFixo() {
        assertThatThrownBy(() -> politica.validarParaReserva(
                AGORA.minusSeconds(1), AGORA.plusSeconds(3600)))
                .isInstanceOf(ConflitoDeEstadoException.class)
                .hasMessageContaining("passado");
    }

    @Test
    void aceitaHorarioFuturoIndependenteDoFusoDeOrigem() {
        politica.validarParaReserva(
                Instant.parse("2026-11-10T10:00:00Z"),
                Instant.parse("2026-11-10T11:00:00Z"));
    }

    @Test
    void horariosAdjacentesNaoSobrepoemPorqueTerminoEExclusivo() {
        Instant inicio = AGORA.plusSeconds(3600);
        Instant termino = inicio.plusSeconds(3600);

        assertThat(politica.sobrepoe(inicio, termino, termino, termino.plusSeconds(3600))).isFalse();
        assertThat(politica.sobrepoe(inicio, termino, inicio.plusSeconds(1800), termino.plusSeconds(1800))).isTrue();
    }
}
