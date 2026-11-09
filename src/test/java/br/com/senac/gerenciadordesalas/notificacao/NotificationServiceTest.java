package br.com.senac.gerenciadordesalas.notificacao;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-09T22:00:00Z");

    private final NotificationRecordRepository records = mock(NotificationRecordRepository.class);
    private final NotificationGateway gateway = mock(NotificationGateway.class);
    private final NotificationService service = new NotificationService(records, gateway,
            Clock.fixed(AGORA, ZoneOffset.UTC));

    @Test
    void entregaComSucesso_registraSomenteMetadadosSeguros() {
        when(records.save(any(NotificationRecord.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
        when(gateway.deliver(any())).thenReturn(new NotificationGateway.DeliveryResult(
                NotificationGateway.DeliveryResult.Outcome.DELIVERED, 202, null));

        NotificationRecord resultado = service.notificar("RESERVA_APROVADA", "RESERVA", 5L,
                "solicitante@exemplo.test");

        assertThat(resultado.getStatus()).isEqualTo(NotificationStatus.ENVIADA);
        assertThat(resultado.getResponseCode()).isEqualTo(202);
        assertThat(resultado.getCompletedAt()).isEqualTo(AGORA);
    }

    @Test
    void erroDeCliente_naoEhMarcadoComoRetry() {
        when(records.save(any(NotificationRecord.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
        when(gateway.deliver(any())).thenReturn(new NotificationGateway.DeliveryResult(
                NotificationGateway.DeliveryResult.Outcome.PERMANENT_FAILURE, 400, "DESTINO_INVALIDO"));

        NotificationRecord resultado = service.notificar("RESERVA_REJEITADA", "RESERVA", 5L,
                "solicitante@exemplo.test");

        assertThat(resultado.getStatus()).isEqualTo(NotificationStatus.FALHA_PERMANENTE);
        assertThat(resultado.getErrorCode()).isEqualTo("DESTINO_INVALIDO");
    }

    @Test
    void indisponibilidadeExterna_registraFalhaTransitoria() {
        when(records.save(any(NotificationRecord.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
        when(gateway.deliver(any())).thenReturn(new NotificationGateway.DeliveryResult(
                NotificationGateway.DeliveryResult.Outcome.TRANSIENT_FAILURE, null, "TIMEOUT"));

        service.notificar("RESERVA_CANCELADA", "RESERVA", 5L, "solicitante@exemplo.test");

        ArgumentCaptor<NotificationRecord> captor = ArgumentCaptor.forClass(NotificationRecord.class);
        verify(records, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.FALHA_TRANSITORIA);
        assertThat(captor.getValue().getErrorCode()).isEqualTo("TIMEOUT");
    }
}
