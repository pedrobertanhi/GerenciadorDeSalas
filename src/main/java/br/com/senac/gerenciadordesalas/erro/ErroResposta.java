package br.com.senac.gerenciadordesalas.erro;

import java.time.Instant;
import java.util.List;

public record ErroResposta(
        String codigo,
        String mensagem,
        String correlationId,
        Instant timestamp,
        List<CampoErro> fieldErrors
) {
    public ErroResposta(String codigo, String mensagem, String correlationId) {
        this(codigo, mensagem, correlationId, Instant.now(), null);
    }

    public ErroResposta(String codigo, String mensagem, String correlationId, List<CampoErro> fieldErrors) {
        this(codigo, mensagem, correlationId, Instant.now(), fieldErrors);
    }

    public record CampoErro(String campo, String mensagem) {}
}