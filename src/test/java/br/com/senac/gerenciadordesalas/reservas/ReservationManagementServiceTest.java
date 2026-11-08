package br.com.senac.gerenciadordesalas.reservas;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import br.com.senac.gerenciadordesalas.salas.Sala;
import br.com.senac.gerenciadordesalas.salas.SalaRepository;
import br.com.senac.gerenciadordesalas.manutencao.MaintenanceBlockService;
import br.com.senac.gerenciadordesalas.usuario.Role;
import br.com.senac.gerenciadordesalas.usuario.User;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationManagementServiceTest {

    private final ReservationRepository reservas = Mockito.mock(ReservationRepository.class);
    private final UserRepository usuarios = Mockito.mock(UserRepository.class);
    private final SalaRepository salas = Mockito.mock(SalaRepository.class);
    private final br.com.senac.gerenciadordesalas.professor.ProfessorRepository professores =
            Mockito.mock(br.com.senac.gerenciadordesalas.professor.ProfessorRepository.class);
    private final ReservationManagementService service = new ReservationManagementService(
            reservas, usuarios, salas,
            professores,
            Mockito.mock(br.com.senac.gerenciadordesalas.material.MaterialRepository.class),
            new ReservationTimePolicy(java.time.Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"), ZoneOffset.UTC)),
            Mockito.mock(MaintenanceBlockService.class));

    private User dono;
    private Sala sala;
    private Reservation reserva;

    @BeforeEach
    void preparar() {
        dono = Mockito.mock(User.class);
        when(dono.getId()).thenReturn(1L);
        when(dono.getProfile()).thenReturn(Role.SOLICITANTE);
        when(dono.isActive()).thenReturn(true);

        sala = Mockito.mock(Sala.class);
        when(sala.getId()).thenReturn(10L);
        when(sala.getAtiva()).thenReturn(Boolean.TRUE);

        Instant inicio = Instant.parse("2026-11-10T09:00:00Z");
        reserva = new Reservation(dono, sala, null, inicio, inicio.plusSeconds(7200), "Aula");
        when(reservas.findById(100L)).thenReturn(Optional.of(reserva));
        when(reservas.save(reserva)).thenReturn(reserva);
        when(usuarios.findByEmailIgnoreCase("dono@exemplo.test")).thenReturn(Optional.of(dono));
        when(salas.findById(10L)).thenReturn(Optional.of(sala));
    }

    @Test
    void dono_alteraReservaCriada() {
        ReservationDto.Alteracao alteracao = new ReservationDto.Alteracao(
                10L, null,
                OffsetDateTime.of(2026, 11, 11, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 11, 11, 11, 0, 0, 0, ZoneOffset.UTC),
                "Aula alterada", List.of());

        ReservationDto.Saida saida = service.alterar(100L, alteracao, "dono@exemplo.test");

        assertThat(saida.finalidade()).isEqualTo("Aula alterada");
        // Sem material restrito nem professor, a reserva alterada e aprovada automaticamente (APR-01, #34).
        assertThat(saida.status()).isEqualTo(StatusReserva.APROVADA);
        verify(reservas).save(reserva);
    }

    @Test
    void usuarioDiferente_semPerfilOperacional_naoAlteraNemCancela() {
        User outro = Mockito.mock(User.class);
        when(outro.getId()).thenReturn(2L);
        when(outro.getProfile()).thenReturn(Role.SOLICITANTE);
        when(outro.isActive()).thenReturn(true);
        when(usuarios.findByEmailIgnoreCase("outro@exemplo.test")).thenReturn(Optional.of(outro));

        assertThatThrownBy(() -> service.cancelar(100L, "outro@exemplo.test"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cancelamento_preservaRegistroEAtualizaStatus() {
        ReservationDto.Saida saida = service.cancelar(100L, "dono@exemplo.test");

        assertThat(saida.status()).isEqualTo(StatusReserva.CANCELADA);
        verify(reservas).save(reserva);
    }

    @Test
    void reservaEmAndamento_naoPodeSerCancelada() {
        reserva.transicionarPara(StatusReserva.APROVADA);
        reserva.transicionarPara(StatusReserva.EM_ANDAMENTO);

        assertThatThrownBy(() -> service.cancelar(100L, "dono@exemplo.test"))
                .isInstanceOf(ConflitoDeEstadoException.class)
                .hasMessageContaining("nao pode ser cancelada");
    }

    @Test
    void alteracao_travaASalaAntesDeValidarConflitos() {
        ReservationDto.Alteracao alteracao = new ReservationDto.Alteracao(
                10L, null,
                OffsetDateTime.of(2026, 11, 11, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 11, 11, 11, 0, 0, 0, ZoneOffset.UTC),
                "Aula alterada", List.of());

        service.alterar(100L, alteracao, "dono@exemplo.test");

        InOrder ordem = inOrder(reservas);
        ordem.verify(reservas).travarSala(10L);
        ordem.verify(reservas).buscarAtivasDaSalaSobrepondo(
                Mockito.eq(10L), Mockito.any(), Mockito.any());
        ordem.verify(reservas).save(reserva);
    }

    @Test
    void alteracao_travaASalaAntesDeCarregarAReservaEASala() {
        ReservationDto.Alteracao alteracao = new ReservationDto.Alteracao(
                10L, null,
                OffsetDateTime.of(2026, 11, 11, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 11, 11, 11, 0, 0, 0, ZoneOffset.UTC),
                "Aula alterada", List.of());

        service.alterar(100L, alteracao, "dono@exemplo.test");

        InOrder ordem = inOrder(reservas, salas);
        ordem.verify(reservas).travarSala(10L);
        ordem.verify(reservas).findById(100L);
        ordem.verify(salas).findById(10L);
    }

    @Test
    void reservaAprovada_podeSerAlterada_eReavaliaAsPendencias() {
        reserva.reavaliarPendencias();
        assertThat(reserva.getStatus()).isEqualTo(StatusReserva.APROVADA);
        br.com.senac.gerenciadordesalas.professor.Professor professor =
                Mockito.mock(br.com.senac.gerenciadordesalas.professor.Professor.class);
        when(professor.getId()).thenReturn(20L);
        when(professor.isAtivo()).thenReturn(true);
        when(professores.findById(20L)).thenReturn(Optional.of(professor));
        ReservationDto.Alteracao alteracao = new ReservationDto.Alteracao(
                10L, 20L,
                OffsetDateTime.of(2026, 11, 11, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 11, 11, 11, 0, 0, 0, ZoneOffset.UTC),
                "Aula com professor", List.of());

        ReservationDto.Saida saida = service.alterar(100L, alteracao, "dono@exemplo.test");

        assertThat(saida.status()).isEqualTo(StatusReserva.CRIADA);
        assertThat(reserva.getValidacaoDocente()).isEqualTo(SituacaoPendencia.PENDENTE);
    }

    @Test
    void alteracao_travaAReservaAntesDosRecursos() {
        ReservationDto.Alteracao alteracao = new ReservationDto.Alteracao(
                10L, null,
                OffsetDateTime.of(2026, 11, 11, 9, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 11, 11, 11, 0, 0, 0, ZoneOffset.UTC),
                "Aula alterada", List.of());

        service.alterar(100L, alteracao, "dono@exemplo.test");

        InOrder ordem = inOrder(reservas);
        ordem.verify(reservas).travarReserva(100L);
        ordem.verify(reservas).travarSala(10L);
        ordem.verify(reservas).findById(100L);
    }
}
