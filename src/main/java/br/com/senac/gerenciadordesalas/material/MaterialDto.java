package br.com.senac.gerenciadordesalas.material;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public final class MaterialDto {

    private MaterialDto() {
    }

    @Schema(name = "MaterialEntrada", description = "Dados para cadastro ou atualização de material")
    public record Entrada(
            @Schema(example = "Projetor multimídia")
            @NotBlank @Size(max = 150) String nome,
            @Schema(example = "Projetor HDMI de uso compartilhado")
            @Size(max = 500) String descricao,
            @Schema(example = "EQUIPAMENTO")
            @NotNull MaterialTipo tipo,
            @Schema(description = "Código patrimonial opcional e único", example = "PAT-2026-001")
            @Size(max = 100) String patrimonio,
            @Schema(description = "Quantidade total cadastrada", example = "4", minimum = "0")
            @NotNull @PositiveOrZero Integer quantidadeTotal,
            @Schema(example = "Almoxarifado A")
            @NotBlank @Size(max = 150) String localizacao,
            @Schema(description = "Indica se a reserva exige aprovação", example = "true")
            @NotNull Boolean restrito) {
    }

    @Schema(name = "MaterialSaida", description = "Material persistido")
    public record Saida(
            Long id,
            String nome,
            String descricao,
            MaterialTipo tipo,
            String patrimonio,
            int quantidadeTotal,
            String localizacao,
            boolean restrito,
            boolean ativo) {

        public static Saida de(Material material) {
            return new Saida(
                    material.getId(),
                    material.getNome(),
                    material.getDescricao(),
                    material.getTipo(),
                    material.getPatrimonio(),
                    material.getQuantidadeTotal(),
                    material.getLocalizacao(),
                    material.isRestrito(),
                    material.isAtivo());
        }
    }
}
