package br.com.senac.gerenciadordesalas.manutencao;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import br.com.senac.gerenciadordesalas.erro.RecursoNaoEncontradoException;
import br.com.senac.gerenciadordesalas.material.MaterialRepository;
import br.com.senac.gerenciadordesalas.professor.ProfessorRepository;
import br.com.senac.gerenciadordesalas.salas.SalaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MaintenanceBlockServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-03T15:00:00Z");

    private final MaintenanceBlockRepository bloqueios = mock(MaintenanceBlockRepository.class);
    private final SalaRepository salas = mock(SalaRepository.class);
    private final ProfessorRepository professores = mock(ProfessorRepository.class);
    private final MaterialRepository materiais = mock(MaterialRepository.class);
    private final MaintenanceBlockService servico = new MaintenanceBlockService(
            bloqueios, salas, professores, materiais, Clock.fixed(AGORA, ZoneOffset.UTC));

    @BeforeEach
    void gravarDevolvendoOMesmoObjeto() {
        when(bloqueios.saveAndFlush(any(MaintenanceBlock.class))).thenAnswer(i -> i.getArgument(0));
    }

    private BloqueioDto.Entrada entrada(TipoRecurso tipo, Long recursoId, String motivo) {
        return new BloqueioDto.Entrada(tipo, recursoId, motivo,
                OffsetDateTime.ofInstant(AGORA.plusSeconds(3600), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(AGORA.plusSeconds(18000), ZoneOffset.UTC));
    }

    private MaintenanceBlock existente(Instant inicio) {
        MaintenanceBlock bloqueio = new MaintenanceBlock(TipoRecurso.SALA, 12L, "Troca do projetor",
                inicio, AGORA.plusSeconds(18000), AGORA.minusSeconds(86400));
        when(bloqueios.findById(1L)).thenReturn(Optional.of(bloqueio));
        return bloqueio;
    }

    @ParameterizedTest
    @EnumSource(TipoRecurso.class)
    void criarConsultaSomenteORepositorioDoTipoInformado(TipoRecurso tipo) {
        when(salas.existsById(12L)).thenReturn(true);
        when(professores.existsById(12L)).thenReturn(true);
        when(materiais.existsById(12L)).thenReturn(true);

        BloqueioDto.Saida saida = servico.criar(entrada(tipo, 12L, "Revisao"));

        assertThat(saida.status()).isEqualTo(StatusBloqueio.ATIVO);
        assertThat(saida.recursoTipo()).isEqualTo(tipo);
        verify(salas, times(tipo == TipoRecurso.SALA ? 1 : 0)).existsById(12L);
        verify(professores, times(tipo == TipoRecurso.PROFESSOR ? 1 : 0)).existsById(12L);
        verify(materiais, times(tipo == TipoRecurso.MATERIAL ? 1 : 0)).existsById(12L);
    }

    @Test
    void recursoInexistenteEhRecusadoSemGravar() {
        assertThatThrownBy(() -> servico.criar(entrada(TipoRecurso.SALA, 999L, "Revisao")))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(bloqueios, never()).saveAndFlush(any(MaintenanceBlock.class));
    }

    @Test
    void criarUsaORelogioNosTimestampsENormalizaOMotivo() {
        when(salas.existsById(12L)).thenReturn(true);

        servico.criar(entrada(TipoRecurso.SALA, 12L, "  Troca do projetor  "));

        ArgumentCaptor<MaintenanceBlock> gravado = ArgumentCaptor.forClass(MaintenanceBlock.class);
        verify(bloqueios).saveAndFlush(gravado.capture());
        assertThat(gravado.getValue().getMotivo()).isEqualTo("Troca do projetor");
        assertThat(gravado.getValue().getCriadoEm()).isEqualTo(AGORA);
        assertThat(gravado.getValue().getInicio()).isEqualTo(AGORA.plusSeconds(3600));
    }

    @Test
    void atualizarGravaMotivoEPeriodoENaoTrocaORecurso() {
        MaintenanceBlock bloqueio = existente(AGORA.plusSeconds(3600));

        BloqueioDto.Saida saida = servico.atualizar(1L, entrada(TipoRecurso.SALA, 12L, "Novo motivo"));

        assertThat(saida.motivo()).isEqualTo("Novo motivo");
        assertThat(bloqueio.getAtualizadoEm()).isEqualTo(AGORA);
    }

    @Test
    void atualizarComOutroRecursoEhRecusado() {
        existente(AGORA.plusSeconds(3600));

        assertThatThrownBy(() -> servico.atualizar(1L, entrada(TipoRecurso.SALA, 13L, "Motivo")))
                .isInstanceOf(ConflitoDeEstadoException.class);
        assertThatThrownBy(() -> servico.atualizar(1L, entrada(TipoRecurso.MATERIAL, 12L, "Motivo")))
                .isInstanceOf(ConflitoDeEstadoException.class);
        verify(bloqueios, never()).saveAndFlush(any(MaintenanceBlock.class));
    }

    @Test
    void concluirUsaORelogioParaRegistrarOEncerramento() {
        existente(AGORA.minusSeconds(3600));

        BloqueioDto.Saida saida = servico.concluir(1L);

        assertThat(saida.status()).isEqualTo(StatusBloqueio.CONCLUIDO);
        assertThat(saida.encerradoEm().toInstant()).isEqualTo(AGORA);
    }

    @Test
    void concluirBloqueioQueAindaNaoComecouEhRecusadoSemGravar() {
        existente(AGORA.plusSeconds(3600));

        assertThatThrownBy(() -> servico.concluir(1L)).isInstanceOf(ConflitoDeEstadoException.class);

        verify(bloqueios, never()).saveAndFlush(any(MaintenanceBlock.class));
    }

    @Test
    void cancelarBloqueioAtivoRegistraOEncerramento() {
        existente(AGORA.plusSeconds(3600));

        BloqueioDto.Saida saida = servico.cancelar(1L);

        assertThat(saida.status()).isEqualTo(StatusBloqueio.CANCELADO);
        assertThat(saida.encerradoEm().toInstant()).isEqualTo(AGORA);
    }

    @Test
    void bloqueioInexistenteEhRecusadoEmTodasAsOperacoes() {
        assertThatThrownBy(() -> servico.consultar(99L)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> servico.concluir(99L)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> servico.cancelar(99L)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> servico.atualizar(99L, entrada(TipoRecurso.SALA, 12L, "Motivo")))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void listarRepassaOsFiltrosAoRepositorio() {
        MaintenanceBlock bloqueio = existente(AGORA.plusSeconds(3600));
        when(bloqueios.buscar(TipoRecurso.SALA, 12L, StatusBloqueio.ATIVO)).thenReturn(List.of(bloqueio));

        List<BloqueioDto.Saida> lista = servico.listar(TipoRecurso.SALA, 12L, StatusBloqueio.ATIVO);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).motivo()).isEqualTo("Troca do projetor");
    }
}