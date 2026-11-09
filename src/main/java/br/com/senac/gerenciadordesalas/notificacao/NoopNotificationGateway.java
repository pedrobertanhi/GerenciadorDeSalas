package br.com.senac.gerenciadordesalas.notificacao;

import org.springframework.stereotype.Component;

/**
 * Comportamento seguro até que um gateway HTTP seja configurado: registra a
 * tentativa como transitória, sem chamar serviço externo nem perder o evento.
 */
@Component
class NoopNotificationGateway implements NotificationGateway {

    @Override
    public DeliveryResult deliver(NotificationCommand command) {
        return new DeliveryResult(DeliveryResult.Outcome.TRANSIENT_FAILURE,
                null, "GATEWAY_NAO_CONFIGURADO");
    }
}
