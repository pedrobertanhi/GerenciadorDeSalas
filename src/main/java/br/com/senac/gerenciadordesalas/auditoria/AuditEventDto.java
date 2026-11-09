package br.com.senac.gerenciadordesalas.auditoria;

import java.time.Instant;
import java.util.Map;

public final class AuditEventDto {

    private AuditEventDto() {
    }

    public record Item(
            Long id,
            String entidade,
            Long entidadeId,
            TipoAcaoAuditoria acao,
            Long atorId,
            String atorNome,
            Instant ocorridoEm,
            String estadoAnterior,
            String estadoNovo,
            Map<String, Object> detalhes
    ) {
        static Item de(AuditEvent evento) {
            return new Item(
                    evento.getId(),
                    evento.getEntity(),
                    evento.getEntityId(),
                    evento.getAction(),
                    evento.getActor() == null ? null : evento.getActor().getId(),
                    evento.getActor() == null ? null : evento.getActor().getName(),
                    evento.getOccurredAt(),
                    evento.getPreviousState(),
                    evento.getNewState(),
                    evento.getDetails()
            );
        }
    }
}
