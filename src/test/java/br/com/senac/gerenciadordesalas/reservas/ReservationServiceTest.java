package br.com.senac.gerenciadordesalas.reservas;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import br.com.senac.gerenciadordesalas.erro.RecursoNaoEncontradoException;
import br.com.senac.gerenciadordesalas.material.Material;
import br.com.senac.gerenciadordesalas.material.MaterialRepository;
import br.com.senac.gerenciadordesalas.professor.Professor;
import br.com.senac.gerenciadordesalas.professor.ProfessorRepository;
import br.com.senac.gerenciadordesalas.salas.Sala;
import br.com.senac.gerenciadordesalas.salas.SalaRepository;
import br.com.senac.gerenciadordesalas.usuario.User;
import br.com.senac.gerenciadordesalas.usuario.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Regras do {@link ReservationService#criar} (RSV-02, #24):
 * dono vem do usuario autenticado, recursos inativos ou inexistentes sao rejeitados,
 * materiais duplicados e quantidade acima do saldo nao passam.
 */
class ReservationServiceTest {

    private final ReservationRepository reservas = Mockito.mock(ReservationRepository.class);
    private final UserRepository usuarios = Mockito.mock(UserRepository.class);
    private final SalaRepository salas = Mockito.mock(SalaRepository.class);
    private final ProfessorRepository professores = Mockito.mock(ProfessorRepository.class);
    private final MaterialRepository materiais = Mockito.mock(MaterialRepository.class);

    private final ReservationService service = new ReservationService(
            reservas, usuarios, salas, professores, materiais,
            new ReservationTimePolicy(Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"), ZoneOffset.UTC)));

    private User solicitante;
    private Sala sala;
    private Professor professor;
    private Material projetor;

    private final OffsetDateTime inicio = OffsetDateTime.of(2026, 11, 10, 9, 0, 0, 0, ZoneOffset.UTC);
    private final OffsetDateTime termino = inicio.plusHours(2);
    private static final String EMAIL = "ana@exemplo.test";

    @BeforeEach
    void preparar() {
        solicitante = Mockito.mock(User.class);
        when(solicitante.isActive()).thenReturn(true);
        when(usuarios.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(solicitante));

        sala = Mockito.mock(Sala.class);
        when(sala.getId()).thenReturn(10L);
        when(sala.getAtiva()).thenReturn(Boolean.TRUE);
        when(salas.findById(10L)).thenReturn(Optional.of(sala));

        professor = Mockito.mock(Professor.class);
        when(professor.getId()).thenReturn(20L);
        when(professor.isAtivo()).thenReturn(true);
        when(professores.findById(20L)).thenReturn(Optional.of(professor));

        projetor = Mockito.mock(Material.class);
        when(projetor.getId()).thenReturn(30L);
        when(projetor.getNome()).thenReturn("Projetor");
        when(projetor.isAtivo()).thenReturn(true);
        when(projetor.getQuantidadeTotal()).thenReturn(5);
        when(materiais.findById(30L)).thenReturn(Optional.of(projetor));

        when(reservas.saveAndFlush(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private ReservationDto.Entrada entradaValida() {
        return new ReservationDto.Entrada(10L, 20L, inicio, termino, "Aula de POO",
                List.of(new ReservationDto.MaterialPedido(30L, 2)));
    }

    @Test
    void criar_happyPath_retornaSaidaComFinalidadeEMateriais() {
        ReservationDto.Saida saida = service.criar(entradaValida(), EMAIL);
        assertThat(saida.salaId()).isEqualTo(10L);
        assertThat(saida.professorId()).isEqualTo(20L);
        assertThat(saida.finalidade()).isEqualTo("Aula de POO");
        assertThat(saida.status()).isEqualTo(StatusReserva.CRIADA);
        assertThat(saida.materiais()).hasSize(1);
        assertThat(saida.materiais().get(0).quantidade()).isEqualTo(2);
    }

    @Test
    void criar_semProfessor_aceita() {
        ReservationDto.Entrada entrada = new ReservationDto.Entrada(10L, null, inicio, termino,
                "Reuniao", List.of());
        ReservationDto.Saida saida = service.criar(entrada, EMAIL);
        assertThat(saida.professorId()).isNull();
        assertThat(saida.materiais()).isEmpty();
    }

    @Test
    void criar_solicitanteInexistente_retorna404() {
        when(usuarios.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        assertThatExceptionOfType(RecursoNaoEncontradoException.class)
                .isThrownBy(() -> service.criar(entradaValida(), EMAIL));
    }

    @Test
    void criar_solicitanteInativo_retorna404() {
        when(solicitante.isActive()).thenReturn(false);
        assertThatExceptionOfType(RecursoNaoEncontradoException.class)
                .isThrownBy(() -> service.criar(entradaValida(), EMAIL));
    }

    @Test
    void criar_salaInexistente_retorna404() {
        when(salas.findById(10L)).thenReturn(Optional.empty());
        assertThatExceptionOfType(RecursoNaoEncontradoException.class)
                .isThrownBy(() -> service.criar(entradaValida(), EMAIL));
    }

    @Test
    void criar_salaInativa_retorna404() {
        when(sala.getAtiva()).thenReturn(Boolean.FALSE);
        assertThatExceptionOfType(RecursoNaoEncontradoException.class)
                .isThrownBy(() -> service.criar(entradaValida(), EMAIL));
    }

    @Test
    void criar_professorInexistente_retorna404() {
        when(professores.findById(20L)).thenReturn(Optional.empty());
        assertThatExceptionOfType(RecursoNaoEncontradoException.class)
                .isThrownBy(() -> service.criar(entradaValida(), EMAIL));
    }

    @Test
    void criar_professorInativo_retorna404() {
        when(professor.isAtivo()).thenReturn(false);
        assertThatExceptionOfType(RecursoNaoEncontradoException.class)
                .isThrownBy(() -> service.criar(entradaValida(), EMAIL));
    }

    @Test
    void criar_materialInexistente_retorna404() {
        when(materiais.findById(30L)).thenReturn(Optional.empty());
        assertThatExceptionOfType(RecursoNaoEncontradoException.class)
                .isThrownBy(() -> service.criar(entradaValida(), EMAIL));
    }

    @Test
    void criar_materialInativo_retorna404() {
        when(projetor.isAtivo()).thenReturn(false);
        assertThatExceptionOfType(RecursoNaoEncontradoException.class)
                .isThrownBy(() -> service.criar(entradaValida(), EMAIL));
    }

    @Test
    void criar_materialDuplicadoNoPedido_retorna409() {
        ReservationDto.Entrada entrada = new ReservationDto.Entrada(10L, 20L, inicio, termino,
                "Aula com duplicata",
                List.of(new ReservationDto.MaterialPedido(30L, 1),
                        new ReservationDto.MaterialPedido(30L, 2)));
        assertThatExceptionOfType(ConflitoDeEstadoException.class)
                .isThrownBy(() -> service.criar(entrada, EMAIL));
    }

    @Test
    void criar_quantidadeAcimaDoSaldo_retorna409() {
        ReservationDto.Entrada entrada = new ReservationDto.Entrada(10L, null, inicio, termino,
                "Aula com excesso",
                List.of(new ReservationDto.MaterialPedido(30L, 999)));
        assertThatExceptionOfType(ConflitoDeEstadoException.class)
                .isThrownBy(() -> service.criar(entrada, EMAIL));
    }
}
