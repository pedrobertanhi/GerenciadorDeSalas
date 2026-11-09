package br.com.senac.gerenciadordesalas.notificacao;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Registra toda tentativa e converte o retorno do gateway em estado interno.
 */
@Service
public class NotificationService {

    private final NotificationRecordRepository records;
    private final NotificationGateway gateway;
    private final Clock clock;

    public NotificationService(NotificationRecordRepository records,
                               NotificationGateway gateway, Clock clock) {
        this.records = records;
        this.gateway = gateway;
        this.clock = clock;
    }

    @Transactional
    public NotificationRecord notificar(String evento, String entidade, Long entidadeId,
                                        String destino) {
        Instant agora = Instant.now(clock);
        NotificationRecord record = records.save(
                new NotificationRecord(evento, entidade, entidadeId, destino, agora));
        NotificationGateway.DeliveryResult resultado = gateway.deliver(
                new NotificationGateway.NotificationCommand(evento, entidade, entidadeId, destino));

        switch (resultado.outcome()) {
            case DELIVERED -> record.marcarEnviada(resultado.responseCode(), agora);
            case PERMANENT_FAILURE -> record.marcarFalhaPermanente(resultado.errorCode(), agora);
            case TRANSIENT_FAILURE -> record.marcarFalhaTransitoria(resultado.errorCode(), agora);
        }
        return records.save(record);
    }
}
