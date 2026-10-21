package br.com.senac.gerenciadordesalas.professor;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class ProfessorDto {
    private ProfessorDto() {}
    public record Entrada(
        @NotBlank @Size(max = 150) String nome,
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull Set<@NotBlank @Size(max = 100) String> competencias
    ) {}
    public record Saida(Long id, String nome, String email, boolean ativo, List<String> competencias) {
        public static Saida de(Professor p) {
            return new Saida(p.getId(), p.getNome(), p.getEmail(), p.isAtivo(),
                p.getCompetencias().stream().map(Competencia::getNome).sorted().toList());
        }
    }
}