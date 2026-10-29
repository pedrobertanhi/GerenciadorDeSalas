package br.com.senac.gerenciadordesalas.disponibilidade;

import br.com.senac.gerenciadordesalas.manutencao.*;
import br.com.senac.gerenciadordesalas.material.*;
import br.com.senac.gerenciadordesalas.professor.*;
import br.com.senac.gerenciadordesalas.reservas.*;
import br.com.senac.gerenciadordesalas.salas.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsultaDisponibilidadeServiceTest {
    private final SalaRepository salas = mock(SalaRepository.class);
    private final ProfessorRepository professores = mock(ProfessorRepository.class);
    private final MaterialRepository materiais = mock(MaterialRepository.class);
    private final ReservationRepository reservas = mock(ReservationRepository.class);
    private final MaintenanceBlockRepository manutencoes = mock(MaintenanceBlockRepository.class);
    private final ConsultaDisponibilidadeService service =
            new ConsultaDisponibilidadeService(salas, professores, materiais, reservas, manutencoes);
    private final IntervaloDisponibilidade periodo = new IntervaloDisponibilidade(
            Instant.parse("2026-11-10T12:00:00Z"), Instant.parse("2026-11-10T14:00:00Z"));

    @Test void ignoraSalaInativaEReservaSobreposta() {
        Sala inativa = new Sala("Inativa", 10, "A");
        inativa.setId(1L);
        inativa.setAtiva(false);
        Sala ocupada = new Sala("Ocupada", 10, "A");
        ocupada.setId(2L);
        Sala livre = new Sala("Livre", 10, "A");
        livre.setId(3L);
        when(salas.findAll()).thenReturn(List.of(inativa, ocupada, livre));
        when(reservas.buscarAtivasDaSalaSobrepondo(eq(2L), any(), any()))
                .thenReturn(List.of(mock(Reservation.class)));
        assertEquals(List.of(3L), service.consultar(periodo).salas());
        verify(reservas, never()).buscarAtivasDaSalaSobrepondo(eq(1L), any(), any());
    }

    @Test void bloqueioDeManutencaoExcluiRecurso() {
        Sala sala = new Sala("Bloqueada", 10, "A");
        sala.setId(4L);
        when(salas.findAll()).thenReturn(List.of(sala));
        when(manutencoes.existeBloqueioNoPeriodo(eq(TipoRecurso.SALA), eq(4L), any(), any()))
                .thenReturn(true);
        assertTrue(service.consultar(periodo).salas().isEmpty());
        verify(reservas, never()).buscarAtivasDaSalaSobrepondo(eq(4L), any(), any());
    }

    @Test void materialDisponivelComReservasAdjacentesSemPicoExcessivo() {
        Material material = mock(Material.class);
        when(material.getId()).thenReturn(7L);
        when(material.getQuantidadeTotal()).thenReturn(2);
        when(materiais.buscarAtivos(null, null)).thenReturn(List.of(material));
        Reservation primeira = mock(Reservation.class);
        when(primeira.getInicio()).thenReturn(periodo.inicio());
        when(primeira.getTermino()).thenReturn(Instant.parse("2026-11-10T13:00:00Z"));
        Reservation segunda = mock(Reservation.class);
        when(segunda.getInicio()).thenReturn(Instant.parse("2026-11-10T13:00:00Z"));
        when(segunda.getTermino()).thenReturn(periodo.termino());
        ReservationMaterial a = mock(ReservationMaterial.class);
        when(a.getReserva()).thenReturn(primeira);
        when(a.getQuantidade()).thenReturn(1);
        ReservationMaterial b = mock(ReservationMaterial.class);
        when(b.getReserva()).thenReturn(segunda);
        when(b.getQuantidade()).thenReturn(1);
        when(reservas.materiaisAtivosSobrepondo(eq(7L), any(), any()))
                .thenReturn(List.of(a, b));
        assertEquals(1L, ConsultaDisponibilidadeService.picoSimultaneo(List.of(a, b), periodo));
        assertEquals(List.of(7L), service.consultar(periodo).materiais());
        when(b.getQuantidade()).thenReturn(2);
        assertTrue(service.consultar(periodo).materiais().isEmpty());
    }
}
