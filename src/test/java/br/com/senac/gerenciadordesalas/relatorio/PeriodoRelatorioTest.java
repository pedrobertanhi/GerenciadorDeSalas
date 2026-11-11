package br.com.senac.gerenciadordesalas.relatorio;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Regras do periodo dos relatorios (ENT-03, #185). Roda sem banco. */
class PeriodoRelatorioTest {

    @Test
    void periodoDeUmDia_eValido() {
        PeriodoRelatorio periodo = new PeriodoRelatorio(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 10));

        assertThat(periodo.inicio()).isEqualTo(OffsetDateTime.of(2026, 11, 10, 0, 0, 0, 0, ZoneOffset.ofHours(-3)));
        assertThat(periodo.fimExclusivo()).isEqualTo(OffsetDateTime.of(2026, 11, 11, 0, 0, 0, 0, ZoneOffset.ofHours(-3)));
    }

    @Test
    void dataFinalAntesDaInicial_eRecusada() {
        assertThatThrownBy(() -> new PeriodoRelatorio(LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A data final deve ser igual ou posterior à data inicial.");
    }

    @Test
    void ate366Dias_eAceito() {
        PeriodoRelatorio periodo = new PeriodoRelatorio(LocalDate.of(2027, 1, 1), LocalDate.of(2028, 1, 1));

        assertThat(periodo.ate()).isEqualTo(LocalDate.of(2028, 1, 1));
    }

    @Test
    void maisDe366Dias_eRecusado() {
        assertThatThrownBy(() -> new PeriodoRelatorio(LocalDate.of(2027, 1, 1), LocalDate.of(2028, 1, 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O período pode ter no máximo 366 dias.");
    }

    @Test
    void datasAusentes_saoRecusadas() {
        assertThatThrownBy(() -> new PeriodoRelatorio(null, LocalDate.of(2026, 11, 1)))
                .isInstanceOf(NullPointerException.class).hasMessage("Informe a data inicial.");
        assertThatThrownBy(() -> new PeriodoRelatorio(LocalDate.of(2026, 11, 1), null))
                .isInstanceOf(NullPointerException.class).hasMessage("Informe a data final.");
    }

    @Test
    void doMes_cobreOMesInteiro() {
        PeriodoRelatorio fevereiro = PeriodoRelatorio.doMes(LocalDate.of(2028, 2, 15));

        assertThat(fevereiro.de()).isEqualTo(LocalDate.of(2028, 2, 1));
        assertThat(fevereiro.ate()).isEqualTo(LocalDate.of(2028, 2, 29));
    }
}
