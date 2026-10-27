package br.com.senac.gerenciadordesalas.manutencao;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MaintenanceBlockAvailabilityTest {

    private static final Instant INICIO_BLOQUEIO = Instant.parse("2026-11-03T10:00:00Z");
    private static final Instant FIM_BLOQUEIO = Instant.parse("2026-11-03T12:00:00Z");

    @ParameterizedTest(name = "{0}")
    @MethodSource("periodos")
    void aplicaARegraDeIndisponibilidadeNoIntervaloSemiaberto(
            String cenario, String inicioReserva, String fimReserva, boolean indisponivel) {
        MaintenanceBlock bloqueio = new MaintenanceBlock(
                TipoRecurso.SALA, 12L, "Troca do projetor",
                INICIO_BLOQUEIO, FIM_BLOQUEIO, INICIO_BLOQUEIO.minusSeconds(3600));

        boolean resultado = bloqueio.bloqueiaPeriodo(
                Instant.parse(inicioReserva), Instant.parse(fimReserva));

        assertThat(resultado).isEqualTo(indisponivel);
    }

    private static Stream<Arguments> periodos() {
        return Stream.of(
                Arguments.of("sobreposicao total", "2026-11-03T10:30:00Z", "2026-11-03T11:30:00Z", true),
                Arguments.of("sobreposicao parcial no inicio", "2026-11-03T09:30:00Z", "2026-11-03T10:30:00Z", true),
                Arguments.of("sobreposicao parcial no fim", "2026-11-03T11:30:00Z", "2026-11-03T12:30:00Z", true),
                Arguments.of("adjacente antes", "2026-11-03T09:00:00Z", "2026-11-03T10:00:00Z", false),
                Arguments.of("adjacente depois", "2026-11-03T12:00:00Z", "2026-11-03T13:00:00Z", false));
    }
}
