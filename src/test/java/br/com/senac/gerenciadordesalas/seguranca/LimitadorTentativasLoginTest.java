package br.com.senac.gerenciadordesalas.seguranca;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LimitadorTentativasLoginTest {

    private static final String EMAIL = "ana@exemplo.test";
    private static final String ORIGEM = "192.0.2.10";

    private final RelogioMutavel relogio =
            new RelogioMutavel(Instant.parse("2026-10-20T00:00:00Z"));
    private final LimitadorTentativasLogin limitador =
            new LimitadorTentativasLogin(relogio);

    @Test
    void cincoFalhasAtivamBloqueioTemporario() {
        registrarFalhas(EMAIL, ORIGEM, 4);
        assertThat(limitador.tempoRestante(EMAIL, ORIGEM)).isZero();

        limitador.registrarFalha(EMAIL, ORIGEM);

        assertThat(limitador.tempoRestante(EMAIL, ORIGEM))
                .isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void novaFalhaAposBloqueioAumentaEsperaProgressivamente() {
        registrarFalhas(EMAIL, ORIGEM, 5);
        relogio.avancar(Duration.ofSeconds(31));

        limitador.registrarFalha(EMAIL, ORIGEM);

        assertThat(limitador.tempoRestante(EMAIL, ORIGEM))
                .isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    void estadoExpiraDepoisDaJanelaESemBloqueioPermanente() {
        registrarFalhas(EMAIL, ORIGEM, 5);
        relogio.avancar(Duration.ofMinutes(15));

        assertThat(limitador.tempoRestante(EMAIL, ORIGEM)).isZero();

        limitador.registrarFalha(EMAIL, ORIGEM);
        assertThat(limitador.tempoRestante(EMAIL, ORIGEM)).isZero();
    }

    @Test
    void mesmaContaEhLimitadaMesmoComOrigensDiferentes() {
        for (int tentativa = 1; tentativa <= 5; tentativa++) {
            limitador.registrarFalha(EMAIL, "192.0.2." + tentativa);
        }

        assertThat(limitador.tempoRestante(EMAIL, "198.51.100.20"))
                .isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void mesmaOrigemEhLimitadaMesmoComContasDiferentes() {
        for (int tentativa = 1; tentativa <= 5; tentativa++) {
            limitador.registrarFalha("usuario" + tentativa + "@exemplo.test", ORIGEM);
        }

        assertThat(limitador.tempoRestante("outra@exemplo.test", ORIGEM))
                .isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void loginValidoLimpaEstadoDaContaEDaOrigem() {
        registrarFalhas(EMAIL, ORIGEM, 5);

        limitador.registrarSucesso(EMAIL, ORIGEM);

        assertThat(limitador.tempoRestante(EMAIL, ORIGEM)).isZero();
    }

    private void registrarFalhas(String email, String origem, int quantidade) {
        for (int tentativa = 0; tentativa < quantidade; tentativa++) {
            limitador.registrarFalha(email, origem);
        }
    }

    private static final class RelogioMutavel extends Clock {

        private Instant instante;

        private RelogioMutavel(Instant instante) {
            this.instante = instante;
        }

        void avancar(Duration duracao) {
            instante = instante.plus(duracao);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }
}
