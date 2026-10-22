package br.com.senac.gerenciadordesalas.disponibilidade;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class IntervaloDisponibilidadeTest {
    private IntervaloDisponibilidade intervalo(String inicio, String termino) {
        return new IntervaloDisponibilidade(Instant.parse(inicio), Instant.parse(termino));
    }

    @Test
    void rejeitaIntervalosInvalidos() {
        Instant instante = Instant.parse("2026-10-22T10:00:00Z");
        assertThrows(IllegalArgumentException.class, () -> new IntervaloDisponibilidade(instante, instante));
        assertThrows(IllegalArgumentException.class, () -> new IntervaloDisponibilidade(instante, instante.minusSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new IntervaloDisponibilidade(null, instante));
        assertThrows(IllegalArgumentException.class, () -> IntervaloDisponibilidade.de("2026-10-22T10:00:00", "2026-10-22T11:00:00Z"));
    }

    @Test
    void horariosAdjacentesNaoSeSobrepoem() {
        var primeiro = intervalo("2026-10-22T08:00:00Z", "2026-10-22T10:00:00Z");
        var segundo = intervalo("2026-10-22T10:00:00Z", "2026-10-22T12:00:00Z");
        assertFalse(primeiro.sobrepoe(segundo));
        assertFalse(segundo.sobrepoe(primeiro));
    }

    @Test
    void detectaSobreposicaoParcialETotal() {
        var primeiro = intervalo("2026-10-22T08:00:00Z", "2026-10-22T10:00:00Z");
        assertTrue(primeiro.sobrepoe(intervalo("2026-10-22T09:59:00Z", "2026-10-22T12:00:00Z")));
        assertTrue(primeiro.sobrepoe(intervalo("2026-10-22T08:00:00Z", "2026-10-22T10:00:00Z")));
    }

    @Test
    void normalizaDeslocamentosParaMesmoInstante() {
        var local = IntervaloDisponibilidade.de("2026-10-22T08:00:00-03:00", "2026-10-22T10:00:00-03:00");
        var utc = IntervaloDisponibilidade.de("2026-10-22T11:00:00Z", "2026-10-22T13:00:00Z");
        assertEquals(utc, local);
    }
}
