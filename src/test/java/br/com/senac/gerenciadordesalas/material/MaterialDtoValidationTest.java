package br.com.senac.gerenciadordesalas.material;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaterialDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejeitaQuantidadeNegativa() {
        var entrada = validaComQuantidade(-1);

        assertThat(validator.validate(entrada))
                .extracting(violacao -> violacao.getPropertyPath().toString())
                .contains("quantidadeTotal");
    }

    @Test
    void aceitaQuantidadeZeroEPatrimonioAusente() {
        var entrada = validaComQuantidade(0);

        assertThat(validator.validate(entrada)).isEmpty();
    }

    @Test
    void exigeNomeTipoLocalizacaoEIndicadorDeRestricao() {
        var entrada = new MaterialDto.Entrada(" ", null, null, null, 1, " ", null);

        assertThat(validator.validate(entrada))
                .extracting(violacao -> violacao.getPropertyPath().toString())
                .contains("nome", "tipo", "localizacao", "restrito");
    }

    private MaterialDto.Entrada validaComQuantidade(int quantidade) {
        return new MaterialDto.Entrada(
                "Cabo HDMI",
                null,
                MaterialTipo.MATERIAL,
                null,
                quantidade,
                "Almoxarifado",
                false);
    }
}
