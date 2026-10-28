package br.com.senac.gerenciadordesalas.reservas;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Contratos de entrada e saida da API de reservas (RSV-02, #24).
 * Entrada aceita datas em ISO 8601 com deslocamento (OffsetDateTime); saida
 * expoe apenas os campos seguros do agregado.
 */
public final class ReservationDto {

    private ReservationDto() {
    }

    public record Entrada(
            @NotNull(message = "sala e obrigatoria") Long salaId,
            Long professorId,
            @NotNull(message = "inicio e obrigatorio") OffsetDateTime inicio,
            @NotNull(message = "termino e obrigatorio") OffsetDateTime termino,
            @NotBlank(message = "finalidade e obrigatoria")
            @Size(max = 500, message = "finalidade deve ter no maximo 500 caracteres") String finalidade,
            List<@NotNull(message = "item de material nao pode ser nulo") @Valid MaterialPedido> materiais) {

        public Instant inicioInstant() {
            return inicio.toInstant();
        }

        public Instant terminoInstant() {
            return termino.toInstant();
        }
        
        @JsonIgnore
        @AssertTrue(message = "termino deve ser posterior ao inicio")
        public boolean isPeriodoValido() {
            return inicio == null || termino == null || termino.isAfter(inicio);
        }

        public List<MaterialPedido> materiaisOuVazio() {
            return materiais == null ? List.of() : materiais;
        }
    }

    public record MaterialPedido(
            @NotNull(message = "material e obrigatorio") Long materialId,
            @Positive(message = "quantidade deve ser maior que zero") int quantidade) {
    }

    public record Saida(
            Long id,
            Long versao,
            Long solicitanteId,
            Long salaId,
            Long professorId,
            Instant inicio,
            Instant termino,
            String finalidade,
            StatusReserva status,
            List<MaterialReservado> materiais,
            Instant criadoEm,
            Instant atualizadoEm) {

        public static Saida de(Reservation reserva) {
            List<MaterialReservado> itens = reserva.getMateriais().stream()
                    .map(m -> new MaterialReservado(
                            m.getMaterial().getId(), m.getMaterial().getNome(), m.getQuantidade()))
                    .toList();
            return new Saida(
                    reserva.getId(),
                    reserva.getVersao(),
                    reserva.getSolicitante().getId(),
                    reserva.getSala().getId(),
                    reserva.getProfessor() == null ? null : reserva.getProfessor().getId(),
                    reserva.getInicio(),
                    reserva.getTermino(),
                    reserva.getFinalidade(),
                    reserva.getStatus(),
                    itens,
                    reserva.getCriadoEm(),
                    reserva.getAtualizadoEm());
        }
    }

    public record MaterialReservado(Long id, String nome, int quantidade) {
    }
}
