package br.com.senac.gerenciadordesalas.disponibilidade;

import java.time.Instant;

/** Resposta explicita enquanto os provedores de conflito nao existem. */
public record ConsultaDisponibilidadeResposta(
        String codigo, String mensagem, Instant inicio, Instant termino) {
}
