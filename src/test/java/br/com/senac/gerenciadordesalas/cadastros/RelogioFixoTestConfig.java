package br.com.senac.gerenciadordesalas.cadastros;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Relogio fixo para os testes de integracao de reservas (RSV-05, #27).
 * A politica temporal recusa reserva no passado; com o relogio real, datas
 * fixas nos testes vencem com o tempo e quebram a CI. Com este relogio,
 * "agora" e sempre 01/11/2026 00:00 UTC, entao o resultado nao depende do
 * dia em que a suite roda.
 */
@TestConfiguration(proxyBeanMethods = false)
class RelogioFixoTestConfig {

    static final Instant AGORA = Instant.parse("2026-11-01T00:00:00Z");

    @Bean
    @Primary
    Clock relogioFixoDosTestes() {
        return Clock.fixed(AGORA, ZoneOffset.UTC);
    }
}