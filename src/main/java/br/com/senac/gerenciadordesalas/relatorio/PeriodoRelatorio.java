package br.com.senac.gerenciadordesalas.relatorio;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;

/**
 * Periodo explicito dos relatorios (ENT-03, #185; RF-12): datas inteiras, inicio e fim inclusivos,
 * no fuso do negocio. Na consulta vira o intervalo semiaberto [inicio, fimExclusivo).
 */
public record PeriodoRelatorio(LocalDate de, LocalDate ate) {

    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    public static final int MAXIMO_DIAS = 366;

    public PeriodoRelatorio {
        Objects.requireNonNull(de, "Informe a data inicial.");
        Objects.requireNonNull(ate, "Informe a data final.");
        if (ate.isBefore(de)) {
            throw new IllegalArgumentException("A data final deve ser igual ou posterior à data inicial.");
        }
        if (ChronoUnit.DAYS.between(de, ate) + 1 > MAXIMO_DIAS) {
            throw new IllegalArgumentException("O período pode ter no máximo " + MAXIMO_DIAS + " dias.");
        }
    }

    /** Mes inteiro que contem o dia informado: o filtro padrao da tela. */
    public static PeriodoRelatorio doMes(LocalDate dia) {
        return new PeriodoRelatorio(dia.withDayOfMonth(1), dia.with(TemporalAdjusters.lastDayOfMonth()));
    }

    public OffsetDateTime inicio() {
        return de.atStartOfDay(FUSO).toOffsetDateTime();
    }

    public OffsetDateTime fimExclusivo() {
        return ate.plusDays(1).atStartOfDay(FUSO).toOffsetDateTime();
    }
}
