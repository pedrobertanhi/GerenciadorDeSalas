package br.com.senac.gerenciadordesalas.disponibilidade;

import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * Intervalo semiaberto [inicio, termino), conforme ADR-003.
 * OffsetDateTime exige deslocamento explícito nas entradas da API.
 */
public record IntervaloDisponibilidade(Instant inicio, Instant termino) {

    public IntervaloDisponibilidade {
        if (inicio == null || termino == null || !termino.isAfter(inicio)) {
            throw new IllegalArgumentException("O termino deve ser posterior ao inicio");
        }
    }

    public static IntervaloDisponibilidade de(String inicio, String termino) {
        if (inicio == null || termino == null) {
            throw new IllegalArgumentException("Inicio e termino sao obrigatorios");
        }
        try {
            return new IntervaloDisponibilidade(
                    OffsetDateTime.parse(inicio).toInstant(),
                    OffsetDateTime.parse(termino).toInstant());
        } catch (java.time.format.DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "Inicio e termino devem estar em ISO 8601 com deslocamento UTC", ex);
        }
    }

    public boolean sobrepoe(IntervaloDisponibilidade outro) {
        return inicio.isBefore(outro.termino()) && termino.isAfter(outro.inicio());
    }
}
