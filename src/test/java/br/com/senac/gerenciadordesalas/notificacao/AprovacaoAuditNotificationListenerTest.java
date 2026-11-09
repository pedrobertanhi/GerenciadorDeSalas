package br.com.senac.gerenciadordesalas.notificacao;

import br.com.senac.gerenciadordesalas.aprovacao.ReservaDecididaEvent;
import br.com.senac.gerenciadordesalas.auditoria.AuditService;
import br.com.senac.gerenciadordesalas.auditoria.TipoAcaoAuditoria;
import br.com.senac.gerenciadordesalas.reservas.Reservation;
import br.com.senac.gerenciadordesalas.reservas.ReservationRepository;
import br.com.senac.gerenciadordesalas.usuario.User;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AprovacaoAuditNotificationListenerTest {

    private final ReservationRepository reservas = mock(ReservationRepository.class);
    private final UserRepository usuarios = mock(UserRepository.class);
    private final AuditService auditoria = mock(AuditService.class);
    private final NotificationService notificacoes = mock(NotificationService.class);
    private final AprovacaoAuditNotificationListener listener =
            new AprovacaoAuditNotificationListener(reservas, usuarios, auditoria, notificacoes);

    @Test
    void decisaoDeRecurso_registraAuditoriaENotificaSolicitante() {
        Reservation reserva = mock(Reservation.class);
        User solicitante = mock(User.class);
        User responsavel = mock(User.class);
        when(reserva.getId()).thenReturn(10L);
        when(reserva.getSolicitante()).thenReturn(solicitante);
        when(solicitante.getEmail()).thenReturn("solicitante@exemplo.test");
        when(reserva.getStatus()).thenReturn(br.com.senac.gerenciadordesalas.reservas.StatusReserva.APROVADA);
        when(reservas.findById(10L)).thenReturn(Optional.of(reserva));
        when(usuarios.findById(7L)).thenReturn(Optional.of(responsavel));

        listener.registrar(new ReservaDecididaEvent(10L,
                ReservaDecididaEvent.TipoDecisao.APROVACAO_RECURSO,
                ReservaDecididaEvent.Resultado.APROVADA, 7L,
                Instant.parse("2026-11-09T22:00:00Z"), null));

        verify(auditoria).registrar(eq("RESERVA"), eq(10L),
                eq(TipoAcaoAuditoria.APROVACAO_RECURSO), eq(responsavel),
                eq("CRIADA"), eq("APROVADA"), any());
        verify(notificacoes).notificar(eq("RESERVA_APROVACAO_RECURSO_APROVADA"),
                eq("RESERVA"), eq(10L), eq("solicitante@exemplo.test"));
    }
}
