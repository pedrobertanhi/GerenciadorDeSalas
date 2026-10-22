package br.com.senac.gerenciadordesalas.salas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SalaRequestDTO(
        @NotBlank(message = "O nome da sala é obrigatório.")
        String nome,

        @NotNull(message = "A capacidade é obrigatória.")
        @Positive(message = "A capacidade deve ser um valor positivo.")
        Integer capacidade,

        @NotBlank(message = "A localização é obrigatória.")
        String localizacao
) {
    public Sala toEntity() {
        return new Sala(this.nome, this.capacidade, this.localizacao);
    }
}