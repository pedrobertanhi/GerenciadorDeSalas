package br.com.senac.gerenciadordesalas.manutencao;

import br.com.senac.gerenciadordesalas.disponibilidade.ConsultaDisponibilidadeResposta;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.OffsetDateTime;

public final class BloqueioDto {

    private BloqueioDto() {
    }

    @Schema(name = "BloqueioManutencaoEntrada",
            description = "Dados para registrar ou alterar um bloqueio de manutenção")
    public record Entrada(
            @Schema(description = "Tipo do recurso bloqueado", example = "SALA")
            @NotNull TipoRecurso recursoTipo,
            @Schema(description = "Identificador do recurso", example = "12", minimum = "1")
            @NotNull @Positive Long recursoId,
            @Schema(example = "Troca do projetor")
            @NotBlank @Size(max = 500) String motivo,
            @Schema(description = "Início, com fuso", example = "2026-11-03T08:00:00-03:00")
            @NotNull OffsetDateTime inicio,
            @Schema(description = "Fim, com fuso; deve ser posterior ao início e ainda estar no futuro",
                    example = "2026-11-03T18:00:00-03:00")
            @NotNull @Future OffsetDateTime fim) {

        @JsonIgnore
        @Schema(hidden = true)
        @AssertTrue(message = "o fim deve ser posterior ao inicio")
        public boolean isPeriodoValido() {
            return inicio == null || fim == null || fim.isAfter(inicio);
        }
    }

    @Schema(name = "BloqueioManutencaoSaida", description = "Bloqueio de manutenção persistido")
    public record Saida(
            Long id,
            TipoRecurso recursoTipo,
            Long recursoId,
            String motivo,
            OffsetDateTime inicio,
            OffsetDateTime fim,
            StatusBloqueio status,
            OffsetDateTime encerradoEm) {

        public static Saida de(MaintenanceBlock bloqueio) {
            return new Saida(
                    bloqueio.getId(),
                    bloqueio.getRecursoTipo(),
                    bloqueio.getRecursoId(),
                    bloqueio.getMotivo(),
                    noFusoLocal(bloqueio.getInicio()),
                    noFusoLocal(bloqueio.getFim()),
                    bloqueio.getStatus(),
                    noFusoLocal(bloqueio.getEncerradoEm()));
        }

        private static OffsetDateTime noFusoLocal(Instant instante) {
            return instante == null ? null : ConsultaDisponibilidadeResposta.noFusoLocal(instante);
        }
    }
}