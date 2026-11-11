package br.com.senac.gerenciadordesalas.relatorio;

import java.util.List;
import java.util.Locale;

/** Dados prontos para a tela de relatorios (ENT-03, #185). */
public final class RelatorioDto {

    /** Limite da lista de reservas na tela; os totais por status continuam contando todas. */
    public static final int LIMITE_LINHAS = 200;

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private RelatorioDto() {
    }

    /** Uso de uma sala no periodo: reservas efetivas e horas reservadas dentro do periodo. */
    public record UsoSala(String sala, String localizacao, boolean ativa, long reservas, double horas) {
        public String horasFormatadas() {
            return String.format(PT_BR, "%.1f h", horas);
        }
    }

    /** Uso de um material no periodo: reservas efetivas que o pediram e quantidade somada. */
    public record UsoMaterial(String material, String tipo, String patrimonio, boolean restrito,
                              long reservas, long quantidade) {
    }

    /** Uma reserva que toca o periodo, em qualquer status. */
    public record LinhaReserva(Long id, String solicitante, String sala, String periodo, String status) {
    }

    public record TotalPorStatus(String status, long total) {
    }

    /**
     * Resultado completo. {@code salas} e {@code materiais} ficam vazios para o Solicitante, que so
     * enxerga as proprias reservas; {@code operacional} diz qual visao foi gerada.
     */
    public record Relatorio(String periodo, boolean operacional, List<UsoSala> salas,
                            List<UsoMaterial> materiais, List<LinhaReserva> reservas,
                            List<TotalPorStatus> totaisPorStatus, long totalReservas, boolean reservasCortadas) {

        public int limiteLinhas() {
            return LIMITE_LINHAS;
        }

        public boolean semUsoDeSalas() {
            return salas.stream().allMatch(uso -> uso.reservas() == 0);
        }
    }
}
