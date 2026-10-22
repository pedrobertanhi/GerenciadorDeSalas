package br.com.senac.gerenciadordesalas.professor;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Set;
import io.swagger.v3.oas.annotations.media.Schema;

public final class ProfessorDto {
    private ProfessorDto() {}
    @Schema(name = "ProfessorEntrada", description = "Cadastro completo; competências é obrigatório e aceita conjunto vazio")
    public record Entrada(
        @Schema(description = "Nome do professor", example = "Professor Exemplo") @NotBlank @Size(max = 150) String nome,
        @Schema(description = "E-mail único", example = "professor@example.test", format = "email") @NotBlank @Email @Size(max = 254) String email,
        @Schema(description = "Nomes de competências; cada item deve ser não vazio e ter até 100 caracteres") @NotNull Set<@NotBlank @Size(max = 100) String> competencias
    ) {}
    @Schema(name = "ProfessorSaida", description = "Cadastro com ID, nome, e-mail, estado ativo e competências em ordem alfabética")
    public record Saida(Long id, String nome, String email, boolean ativo, List<String> competencias) {
        public static Saida de(Professor p) {
            return new Saida(p.getId(), p.getNome(), p.getEmail(), p.isAtivo(),
                p.getCompetencias().stream().map(Competencia::getNome).sorted().toList());
        }
    }
}
