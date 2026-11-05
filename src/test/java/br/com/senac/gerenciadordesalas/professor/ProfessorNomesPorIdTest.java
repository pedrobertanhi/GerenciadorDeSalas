package br.com.senac.gerenciadordesalas.professor;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Nomes de professores por identificador (tela de manutenção, #15): o histórico continua mostrando
 * o professor mesmo depois que ele é inativado.
 */
class ProfessorNomesPorIdTest {

    private final ProfessorRepository professores = mock(ProfessorRepository.class);
    private final ProfessorService service = new ProfessorService(professores, mock(CompetenciaRepository.class));

    private static Professor professor(Long id, String nome) {
        Professor professor = mock(Professor.class);
        when(professor.getId()).thenReturn(id);
        when(professor.getNome()).thenReturn(nome);
        return professor;
    }

    @Test
    void devolveONomeDeCadaProfessorPeloIdentificador() {
        when(professores.findAllById(Set.of(1L, 2L)))
                .thenReturn(List.of(professor(1L, "Ana Souza"), professor(2L, "Bruno Lima")));

        assertThat(service.nomesPorId(Set.of(1L, 2L)))
                .containsExactlyInAnyOrderEntriesOf(Map.of(1L, "Ana Souza", 2L, "Bruno Lima"));
    }

    @Test
    void incluiProfessorInativadoPoisNaoFiltraPorSituacao() {
        Professor inativo = professor(3L, "Carlos Inativo");
        when(inativo.isAtivo()).thenReturn(false);
        when(professores.findAllById(Set.of(3L))).thenReturn(List.of(inativo));

        assertThat(service.nomesPorId(Set.of(3L))).containsEntry(3L, "Carlos Inativo");
        verify(professores).findAllById(Set.of(3L));
    }

    @Test
    void identificadorSemCadastroNaoApareceNoResultado() {
        when(professores.findAllById(Set.of(9L))).thenReturn(List.of());

        assertThat(service.nomesPorId(Set.of(9L))).isEmpty();
    }

    @Test
    void semIdentificadoresNaoConsultaOBanco() {
        assertThat(service.nomesPorId(Set.of())).isEmpty();

        verifyNoInteractions(professores);
    }
}