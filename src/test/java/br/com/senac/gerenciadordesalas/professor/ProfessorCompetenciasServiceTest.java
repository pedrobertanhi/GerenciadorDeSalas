package br.com.senac.gerenciadordesalas.professor;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Inclusao e remocao individual de competencias (ENT-03, #185). Roda sem banco. */
class ProfessorCompetenciasServiceTest {

    @Mock ProfessorRepository professores;
    @Mock CompetenciaRepository competencias;
    private ProfessorService service;
    private Professor professor;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        service = new ProfessorService(professores, competencias);
        professor = new Professor("Ana", "ana@exemplo.test", Set.of(new Competencia("matemática")));
        when(professores.findById(1L)).thenReturn(Optional.of(professor));
        when(professores.saveAndFlush(any(Professor.class))).thenAnswer(i -> i.getArgument(0));
        when(competencias.saveAndFlush(any(Competencia.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void incluiCompetenciaNovaNormalizada() {
        when(competencias.findByNome("física")).thenReturn(Optional.empty());

        var saida = service.adicionarCompetencia(1L, "  Física ");

        assertThat(saida.competencias()).containsExactly("física", "matemática");
        verify(competencias).saveAndFlush(any(Competencia.class));
    }

    @Test
    void reaproveitaCompetenciaJaCadastrada() {
        when(competencias.findByNome("química")).thenReturn(Optional.of(new Competencia("química")));

        var saida = service.adicionarCompetencia(1L, "Química");

        assertThat(saida.competencias()).containsExactly("matemática", "química");
        verify(competencias, never()).saveAndFlush(any(Competencia.class));
    }

    @Test
    void recusaCompetenciaQueOProfessorJaPossui() {
        assertThatThrownBy(() -> service.adicionarCompetencia(1L, " MATEMÁTICA "))
                .isInstanceOf(ConflitoDeEstadoException.class)
                .hasMessage("O professor já possui a competência \"matemática\".");
        verify(professores, never()).saveAndFlush(any(Professor.class));
    }

    @Test
    void recusaCompetenciaVaziaOuLongaSemGravar() {
        assertThatThrownBy(() -> service.adicionarCompetencia(1L, "   "))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Informe a competência.");
        assertThatThrownBy(() -> service.adicionarCompetencia(1L, "x".repeat(101)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A competência pode ter no máximo 100 caracteres.");
        verify(professores, never()).saveAndFlush(any(Professor.class));
    }

    @Test
    void removeCompetenciaIgnorandoMaiusculas() {
        var saida = service.removerCompetencia(1L, "Matemática");

        assertThat(saida.competencias()).isEmpty();
    }

    @Test
    void recusaRemoverCompetenciaQueOProfessorNaoPossui() {
        assertThatThrownBy(() -> service.removerCompetencia(1L, "história"))
                .isInstanceOf(ConflitoDeEstadoException.class)
                .hasMessage("O professor não possui a competência \"história\".");
        verify(professores, never()).saveAndFlush(any(Professor.class));
    }

    @Test
    void professorInativoNaoTemCompetenciasAlteradas() {
        professor.desativar();

        assertThatThrownBy(() -> service.adicionarCompetencia(1L, "física"))
                .isInstanceOf(ConflitoDeEstadoException.class)
                .hasMessage("Professor inativo não pode ter as competências alteradas.");
        assertThatThrownBy(() -> service.removerCompetencia(1L, "matemática"))
                .isInstanceOf(ConflitoDeEstadoException.class);
    }
}
