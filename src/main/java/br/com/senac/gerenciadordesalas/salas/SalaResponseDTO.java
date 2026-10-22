package br.com.senac.gerenciadordesalas.salas;

public record SalaResponseDTO(
        Long id,
        String nome,
        Integer capacidade,
        String localizacao,
        Boolean ativa
) {
    public SalaResponseDTO(Sala sala) {
        this(sala.getId(), sala.getNome(), sala.getCapacidade(), sala.getLocalizacao(), sala.getAtiva());
    }
}