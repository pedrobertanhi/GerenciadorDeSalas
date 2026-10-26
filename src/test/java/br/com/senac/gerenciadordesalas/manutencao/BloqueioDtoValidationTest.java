package br.com.senac.gerenciadordesalas.manutencao;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BloqueioDtoValidationTest {

    private static Validator validator;

    private final OffsetDateTime agora = OffsetDateTime.now();

    @BeforeAll
    static void criarValidador() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private BloqueioDto.Entrada entrada(TipoRecurso tipo, Long recursoId, String motivo,
                                        OffsetDateTime inicio, OffsetDateTime fim) {
        return new BloqueioDto.Entrada(tipo, recursoId, motivo, inicio, fim);
    }

    private BloqueioDto.Entrada valida() {
        return entrada(TipoRecurso.SALA, 12L, "Troca do projetor", agora.plusDays(1), agora.plusDays(2));
    }

    private Set<String> propriedadesComErro(BloqueioDto.Entrada entrada) {
        Set<ConstraintViolation<BloqueioDto.Entrada>> violacoes = validator.validate(entrada);
        return violacoes.stream().map(v -> v.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }

    @Test
    void entradaValidaNaoTemViolacoes() {
        assertThat(validator.validate(valida())).isEmpty();
    }

    @Test
    void manutencaoJaEmAndamentoEhAceita() {
        BloqueioDto.Entrada emAndamento =
                entrada(TipoRecurso.MATERIAL, 3L, "Revisao", agora.minusDays(1), agora.plusDays(1));

        assertThat(validator.validate(emAndamento)).isEmpty();
    }

    @Test
    void fimIgualAoInicioEhRecusado() {
        OffsetDateTime instante = agora.plusDays(1);

        assertThat(propriedadesComErro(entrada(TipoRecurso.SALA, 12L, "Motivo", instante, instante)))
                .containsExactly("periodoValido");
    }

    @Test
    void fimAnteriorAoInicioEhRecusado() {
        BloqueioDto.Entrada invertida =
                entrada(TipoRecurso.SALA, 12L, "Motivo", agora.plusDays(3), agora.plusDays(1));

        assertThat(propriedadesComErro(invertida)).containsExactly("periodoValido");
    }

    @Test
    void fimNoPassadoEhRecusado() {
        BloqueioDto.Entrada terminada =
                entrada(TipoRecurso.SALA, 12L, "Motivo", agora.minusDays(3), agora.minusDays(1));

        assertThat(propriedadesComErro(terminada)).containsExactly("fim");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void motivoEmBrancoEhRecusado(String motivo) {
        BloqueioDto.Entrada entrada =
                entrada(TipoRecurso.SALA, 12L, motivo, agora.plusDays(1), agora.plusDays(2));

        assertThat(propriedadesComErro(entrada)).containsExactly("motivo");
    }

    @Test
    void motivoComMaisDe500CaracteresEhRecusadoEComExatamente500EhAceito() {
        String com500 = "a".repeat(500);
        String com501 = "a".repeat(501);

        assertThat(propriedadesComErro(
                entrada(TipoRecurso.SALA, 12L, com500, agora.plusDays(1), agora.plusDays(2)))).isEmpty();
        assertThat(propriedadesComErro(
                entrada(TipoRecurso.SALA, 12L, com501, agora.plusDays(1), agora.plusDays(2))))
                .containsExactly("motivo");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void recursoIdNaoPositivoEhRecusado(long recursoId) {
        BloqueioDto.Entrada entrada =
                entrada(TipoRecurso.SALA, recursoId, "Motivo", agora.plusDays(1), agora.plusDays(2));

        assertThat(propriedadesComErro(entrada)).containsExactly("recursoId");
    }

    @Test
    void camposNulosSaoRecusadosSemAcusarPeriodoInvalido() {
        assertThat(propriedadesComErro(entrada(null, null, null, null, null)))
                .containsExactlyInAnyOrder("recursoTipo", "recursoId", "motivo", "inicio", "fim");
    }
}