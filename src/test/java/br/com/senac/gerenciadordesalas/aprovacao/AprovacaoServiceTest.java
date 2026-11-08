package br.com.senac.gerenciadordesalas.aprovacao;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import br.com.senac.gerenciadordesalas.erro.RecursoNaoEncontradoException;
import br.com.senac.gerenciadordesalas.manutencao.MaintenanceBlockService;
import br.com.senac.gerenciadordesalas.manutencao.TipoRecurso;
import br.com.senac.gerenciadordesalas.material.Material;
import br.com.senac.gerenciadordesalas.professor.Professor;
import br.com.senac.gerenciadordesalas.reservas.Reservation;
import br.com.senac.gerenciadordesalas.reservas.ReservationDto;
import br.com.senac.gerenciadordesalas.reservas.ReservationRepository;
import br.com.senac.gerenciadordesalas.reservas.SituacaoPendencia;
import br.com.senac.gerenciadordesalas.reservas.StatusReserva;
import br.com.senac.gerenciadordesalas.salas.Sala;
import br.com.senac.gerenciadordesalas.usuario.Role;
import br.com.senac.gerenciadordesalas.usuario.User;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Decisoes do Responsavel (APR-02, #34): aprovar, rejeitar com motivo, impedir decisao
 * repetida, revalidar conflito surgido depois da solicitacao e travar antes de decidir.
 */
class AprovacaoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-05T12:00:00Z");
    private static final String EMAIL_RESPONSAVEL = "responsavel@exemplo.test";
    private static final Instant INICIO = Instant.parse("2026-11-20T10:00:00Z");
    private static final Instant TERMINO = Instant.parse("2026-11-20T12:00:00Z");

    private final ReservationRepository reservas = Mockito.mock(ReservationRepository.class);
    private final UserRepository usuarios = Mockito.mock(UserRepository.class);
    private final MaintenanceBlockService manutencoes = Mockito.mock(MaintenanceBlockService.class);
    private final ApplicationEventPublisher eventos = Mockito.mock(ApplicationEventPublisher.class);

    private final AprovacaoService service = new AprovacaoService(
            reservas, usuarios, manutencoes, eventos, Clock.fixed(AGORA, ZoneOffset.UTC));

    private User responsavel;
    private Sala sala;
    private Material microscopio;
    private Reservation reserva;

    @BeforeEach
    void preparar() {
        responsavel = Mockito.mock(User.class);
        when(responsavel.getId()).thenReturn(7L);
        when(responsavel.isActive()).thenReturn(true);
        when(responsavel.getProfile()).thenReturn(Role.RESPONSAVEL);
        when(usuarios.findByEmailIgnoreCase(EMAIL_RESPONSAVEL)).thenReturn(Optional.of(responsavel));

        User dono = Mockito.mock(User.class);
        when(dono.getId()).thenReturn(1L);

        sala = Mockito.mock(Sala.class);
        when(sala.getId()).thenReturn(10L);
        when(sala.getAtiva()).thenReturn(Boolean.TRUE);

        microscopio = Mockito.mock(Material.class);
        when(microscopio.getId()).thenReturn(30L);
        when(microscopio.getNome()).thenReturn("Microscopio");
        when(microscopio.isAtivo()).thenReturn(true);
        when(microscopio.isRestrito()).thenReturn(true);
        when(microscopio.getQuantidadeTotal()).thenReturn(1);

        reserva = new Reservation(dono, sala, null, INICIO, TERMINO, "Aula com microscopio");
        reserva.adicionarMaterial(microscopio, 1);
        reserva.reavaliarPendencias();

        when(reservas.findById(100L)).thenReturn(Optional.of(reserva));
        when(reservas.saveAndFlush(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void aprovar_recursoPendente_aprovaRegistraDecisaoEPublicaEvento() {
        ReservationDto.Saida saida = service.aprovar(100L, EMAIL_RESPONSAVEL);

        assertThat(saida.status()).isEqualTo(StatusReserva.APROVADA);
        assertThat(saida.aprovacaoRecurso()).isEqualTo(SituacaoPendencia.APROVADA);
        assertThat(reserva.getRecursoDecididoPor()).isSameAs(responsavel);
        assertThat(reserva.getRecursoDecididoEm()).isEqualTo(AGORA);

        ArgumentCaptor<ReservaDecididaEvent> evento = ArgumentCaptor.forClass(ReservaDecididaEvent.class);
        verify(eventos).publishEvent(evento.capture());
        assertThat(evento.getValue().tipo()).isEqualTo(ReservaDecididaEvent.TipoDecisao.APROVACAO_RECURSO);
        assertThat(evento.getValue().resultado()).isEqualTo(ReservaDecididaEvent.Resultado.APROVADA);
        assertThat(evento.getValue().responsavelId()).isEqualTo(7L);
        assertThat(evento.getValue().decididaEm()).isEqualTo(AGORA);
    }

    @Test
    void aprovar_travaAReservaAntesDeCarregarEDepoisOsRecursos() {
        service.aprovar(100L, EMAIL_RESPONSAVEL);

        InOrder ordem = inOrder(reservas);
        ordem.verify(reservas).travarReserva(100L);
        ordem.verify(reservas).findById(100L);
        ordem.verify(reservas).travarSala(10L);
        ordem.verify(reservas).travarMateriais(List.of(30L));
        ordem.verify(reservas).saveAndFlush(reserva);
    }

    @Test
    void aprovar_comValidacaoDocentePendente_permaneceCriada() {
        Professor professor = Mockito.mock(Professor.class);
        when(professor.getId()).thenReturn(20L);
        when(professor.isAtivo()).thenReturn(true);
        reserva.alterar(sala, professor, INICIO, TERMINO, "Aula com professor");
        reserva.reavaliarPendencias();

        ReservationDto.Saida saida = service.aprovar(100L, EMAIL_RESPONSAVEL);

        assertThat(saida.status()).isEqualTo(StatusReserva.CRIADA);
        assertThat(saida.aprovacaoRecurso()).isEqualTo(SituacaoPendencia.APROVADA);
        assertThat(saida.validacaoDocente()).isEqualTo(SituacaoPendencia.PENDENTE);
        verify(reservas).travarProfessor(20L);
    }

    @Test
    void rejeitar_preservaMotivoEMudaParaRejeitada() {
        ReservationDto.Saida saida = service.rejeitar(100L, "Microscopio reservado para pesquisa", EMAIL_RESPONSAVEL);

        assertThat(saida.status()).isEqualTo(StatusReserva.REJEITADA);
        assertThat(saida.motivoRejeicao()).isEqualTo("Microscopio reservado para pesquisa");

        ArgumentCaptor<ReservaDecididaEvent> evento = ArgumentCaptor.forClass(ReservaDecididaEvent.class);
        verify(eventos).publishEvent(evento.capture());
        assertThat(evento.getValue().resultado()).isEqualTo(ReservaDecididaEvent.Resultado.REJEITADA);
        assertThat(evento.getValue().motivo()).isEqualTo("Microscopio reservado para pesquisa");
    }

    @Test
    void aprovar_conflitoDeSalaSurgidoDepoisDaSolicitacao_eRecusadoSemSalvar() {
        Reservation outra = Mockito.mock(Reservation.class);
        when(outra.getId()).thenReturn(99L);
        when(reservas.buscarAtivasDaSalaSobrepondo(eq(10L), any(), any())).thenReturn(List.of(outra));

        assertThatThrownBy(() -> service.aprovar(100L, EMAIL_RESPONSAVEL))
                .isInstanceOf(ConflitoDeEstadoException.class)
                .hasMessageContaining("Sala ja possui outra reserva");
        assertThat(reserva.getStatus()).isEqualTo(StatusReserva.CRIADA);
        verify(reservas, never()).saveAndFlush(any());
        verify(eventos, never()).publishEvent(any());
    }

    @Test
    void aprovar_recursoEmManutencao_eRecusado() {
        when(manutencoes.recursoEmManutencao(TipoRecurso.SALA, 10L, INICIO, TERMINO)).thenReturn(true);

        assertThatThrownBy(() -> service.aprovar(100L, EMAIL_RESPONSAVEL))
                .isInstanceOf(ConflitoDeEstadoException.class)
                .hasMessageContaining("manutencao");
        verify(reservas, never()).saveAndFlush(any());
    }

    @Test
    void aprovar_materialSemSaldoNoPeriodo_eRecusado() {
        when(reservas.quantidadeAtivaDoMaterialSobrepondo(eq(30L), any(), any(), any())).thenReturn(1L);

        assertThatThrownBy(() -> service.aprovar(100L, EMAIL_RESPONSAVEL))
                .isInstanceOf(ConflitoDeEstadoException.class)
                .hasMessageContaining("quantidade disponivel");
    }

    @Test
    void decisaoRepetida_eRecusada() {
        service.aprovar(100L, EMAIL_RESPONSAVEL);

        assertThatThrownBy(() -> service.aprovar(100L, EMAIL_RESPONSAVEL))
                .isInstanceOf(ConflitoDeEstadoException.class)
                .hasMessageContaining("nao esta pendente");
        assertThatThrownBy(() -> service.rejeitar(100L, "Tarde demais", EMAIL_RESPONSAVEL))
                .isInstanceOf(ConflitoDeEstadoException.class);
    }

    @Test
    void quemNaoEhResponsavel_naoDecide() {
        when(responsavel.getProfile()).thenReturn(Role.SOLICITANTE);

        assertThatThrownBy(() -> service.aprovar(100L, EMAIL_RESPONSAVEL))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(reserva.getStatus()).isEqualTo(StatusReserva.CRIADA);
    }

    @Test
    void reservaInexistente_retorna404() {
        assertThatThrownBy(() -> service.aprovar(999L, EMAIL_RESPONSAVEL))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
