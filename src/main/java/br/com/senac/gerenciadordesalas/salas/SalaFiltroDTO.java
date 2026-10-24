package br.com.senac.gerenciadordesalas.salas;

import jakarta.validation.constraints.Min;

public record SalaFiltroDTO(
        @Min(value = 1, message = "A capacidade mínima deve ser maior que zero")
        Integer capacidadeMinima,
        String localizacao
) {
}