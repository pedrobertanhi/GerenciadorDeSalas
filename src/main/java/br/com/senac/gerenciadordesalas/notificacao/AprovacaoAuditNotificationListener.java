package br.com.senac.gerenciadordesalas.notificacao;

import br.com.senac.gerenciadordesalas.aprovacao.ReservaDecididaEvent;
import br.com.senac.gerenciadordesalas.auditoria.AuditService;
import br.com.senac.gerenciadordesalas.auditoria.TipoAcaoAuditoria;
import br.com.senac.gerenciadordesalas.reservas.Reservation;
import br.com.senac.gerenciadordesalas.reservas.ReservationRepository;
import br.com.senac.gerenciadordesalas.usuario.User;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Mantém aprovação desacoplada de auditoria/notificação. O listener é síncrono,
 * portanto participa da transação que publicou a decisão.
 */
@Component
@ConditionalOnBean({AuditService.class, NotificationService.class})
class AprovacaoAuditNotificationListener {

    private final ReservationRepository reservas;
    private final UserRepository usuarios;
    private final AuditService auditoria;
    private final NotificationService notificacoes;

    AprovacaoAuditNotificationListener(ReservationRepository reservas, UserRepository usuarios,
                                      AuditService auditoria, NotificationService notificacoes) {
        this.reservas = reservas;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
        this.notificacoes = notificacoes;
    }

    @EventListener
    public void registrar(ReservaDecididaEvent evento) {
        Reservation reserva = reservas.findById(evento.reservaId()).orElse(null);
        if (reserva == null) {
            return;
        }
        User responsavel = usuarios.findById(evento.responsavelId()).orElse(null);
        String acao = evento.tipo().name();
        String novoEstado = evento.resultado().name();

        auditoria.registrar("RESERVA", reserva.getId(), tipo(evento), responsavel,
                "CRIADA", reserva.getStatus().name(),
                Map.of("decisao", acao, "resultado", novoEstado,
                        "motivo", evento.motivo() == null ? "" : evento.motivo()));
        notificacoes.notificar("RESERVA_" + acao + "_" + novoEstado,
                "RESERVA", reserva.getId(), reserva.getSolicitante().getEmail());
    }

    private TipoAcaoAuditoria tipo(ReservaDecididaEvent evento) {
        return evento.tipo() == ReservaDecididaEvent.TipoDecisao.APROVACAO_RECURSO
                ? TipoAcaoAuditoria.APROVACAO_RECURSO
                : TipoAcaoAuditoria.VALIDACAO_DOCENTE;
    }
}
