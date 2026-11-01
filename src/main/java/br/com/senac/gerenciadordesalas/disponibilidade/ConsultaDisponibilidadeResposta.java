package br.com.senac.gerenciadordesalas.disponibilidade;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/** Resposta explicita enquanto os provedores de conflito nao existem. */
public record ConsultaDisponibilidadeResposta(
        String codigo, String mensagem, OffsetDateTime inicio, OffsetDateTime termino,
        List<Long> salas, List<Long> professores, List<Long> materiais) {

    public ConsultaDisponibilidadeResposta(String codigo, String mensagem,
            OffsetDateTime inicio, OffsetDateTime termino) {
        this(codigo, mensagem, inicio, termino, List.of(), List.of(), List.of());
    }


    private static final ZoneId FUSO_NEGOCIO = ZoneId.of("America/Sao_Paulo");

    public static OffsetDateTime noFusoLocal(Instant instante) {
        return instante.atZone(FUSO_NEGOCIO).toOffsetDateTime();
    }
}
