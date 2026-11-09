package br.com.senac.gerenciadordesalas.notificacao;

/**
 * Porta para integração externa. Implementações HTTP devem traduzir erros sem
 * expor token, resposta externa ou detalhes de infraestrutura.
 */
public interface NotificationGateway {
    DeliveryResult deliver(NotificationCommand command);

    record NotificationCommand(String event, String entity, Long entityId, String destination) {
    }

    record DeliveryResult(Outcome outcome, Integer responseCode, String errorCode) {
        public enum Outcome {
            DELIVERED,
            PERMANENT_FAILURE,
            TRANSIENT_FAILURE
        }
    }
}
