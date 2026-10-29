package br.com.senac.gerenciadordesalas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Fonte de tempo da aplicacao. A injecao permite testes determinísticos.
 */
@Configuration
public class TempoConfig {

    @Bean
    public Clock relogioDaAplicacao() {
        return Clock.systemUTC();
    }
}
