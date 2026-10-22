package br.com.senac.gerenciadordesalas.professor;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

class ProfessorDtoValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test void rejeitaEmailInvalido() {
        var entrada = new ProfessorDto.Entrada("Ana", "sem-arroba", Set.of());
        assertThat(validator.validate(entrada)).isNotEmpty();
    }

    @Test void rejeitaNomeEmBranco() {
        var entrada = new ProfessorDto.Entrada("   ", "ana@exemplo.test", Set.of());
        assertThat(validator.validate(entrada)).isNotEmpty();
    }

    @Test void rejeitaCompetenciaVazia() {
        var entrada = new ProfessorDto.Entrada("Ana", "ana@exemplo.test", Set.of(""));
        assertThat(validator.validate(entrada)).isNotEmpty();
    }

    @Test void aceitaCadastroValido() {
        var entrada = new ProfessorDto.Entrada("Ana", "ana@exemplo.test", Set.of("java"));
        assertThat(validator.validate(entrada)).isEmpty();
    }
}