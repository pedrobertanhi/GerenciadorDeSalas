package br.com.senac.gerenciadordesalas.professor;

import br.com.senac.gerenciadordesalas.erro.ConflitoDeEstadoException;
import br.com.senac.gerenciadordesalas.erro.RecursoNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProfessorServiceTest {
    @Mock ProfessorRepository professores;
    @Mock CompetenciaRepository competencias;
    private ProfessorService service;

    @BeforeEach void configurar() {
        MockitoAnnotations.openMocks(this);
        service = new ProfessorService(professores, competencias);
    }

    @Test void rejeitaEmailDuplicadoSemPersistir() {
        when(professores.existsByEmail("ana@exemplo.com")).thenReturn(true);
        assertThatThrownBy(() -> service.criar(new ProfessorDto.Entrada(
            "Ana", " ANA@EXEMPLO.COM ", Set.of())))
            .isInstanceOf(ConflitoDeEstadoException.class);
        verify(professores, never()).saveAndFlush(any());
    }

    @Test void cadastraComEmailNormalizado() {
        when(professores.saveAndFlush(any(Professor.class))).thenAnswer(i -> i.getArgument(0));
        ProfessorDto.Saida resultado = service.criar(new ProfessorDto.Entrada(
            " Ana ", " ANA@EXEMPLO.COM ", Set.of()));
        assertThat(resultado.nome()).isEqualTo("Ana");
        assertThat(resultado.email()).isEqualTo("ana@exemplo.com");
        assertThat(resultado.ativo()).isTrue();
    }

    @Test void consultaInexistenteRetornaErro() {
        when(professores.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.consultar(404L))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test void desativacaoPreservaRegistro() {
        Professor p = new Professor("Ana", "ana@exemplo.com", Set.of());
        when(professores.findById(1L)).thenReturn(Optional.of(p));
        service.desativar(1L);
        assertThat(p.isAtivo()).isFalse();
        verify(professores, never()).delete(any());
    }

    @Test void filtroNormalizaCompetencia() {
        when(professores.buscarAtivos("matematica")).thenReturn(List.of());
        assertThat(service.listar(" MATEMATICA ")).isEmpty();
        verify(professores).buscarAtivos("matematica");
    }
}