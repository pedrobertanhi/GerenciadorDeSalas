package br.com.senac.gerenciadordesalas.auditoria;

import br.com.senac.gerenciadordesalas.usuario.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-09T22:00:00Z");

    private final AuditEventRepository eventos = mock(AuditEventRepository.class);
    private final AuditService service = new AuditService(
            eventos, Clock.fixed(AGORA, ZoneOffset.UTC));

    @Test
    void registrar_mascaraDadosSensiveisEUsaRelogioControlado() {
        User ator = mock(User.class);
        when(eventos.save(any(AuditEvent.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        service.registrar("RESERVA", 12L, TipoAcaoAuditoria.CANCELAMENTO, ator,
                "APROVADA", "CANCELADA",
                Map.of("motivo", "Cancelada pelo solicitante", "token", "nao-pode-vazar"));

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(eventos).save(captor.capture());
        AuditEvent evento = captor.getValue();

        assertThat(evento.getOccurredAt()).isEqualTo(AGORA);
        assertThat(evento.getDetails())
                .containsEntry("motivo", "Cancelada pelo solicitante")
                .containsEntry("token", "[MASCARADO]");
    }

    @Test
    void consultar_recusaPeriodoInvertido() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                service.consultar(null, null, null, AGORA, AGORA.minusSeconds(1),
                        PageRequest.of(0, 20)));
    }

    @Test
    void consultar_retornaPaginaDoRepositorio() {
        when(eventos.findAll(any(), eq(PageRequest.of(0, 20))))
                .thenReturn(Page.empty());

        Page<AuditEventDto.Item> pagina = service.consultar("RESERVA", 12L, null,
                null, null, PageRequest.of(0, 20));

        assertThat(pagina).isEmpty();
    }
}
