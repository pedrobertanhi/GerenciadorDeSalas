package br.com.senac.gerenciadordesalas.aprovacao;

import br.com.senac.gerenciadordesalas.professor.Professor;
import br.com.senac.gerenciadordesalas.reservas.Reservation;
import br.com.senac.gerenciadordesalas.reservas.ReservationMaterial;
import br.com.senac.gerenciadordesalas.reservas.SituacaoPendencia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * Representacao da fila de aprovacao (APR-01, #34): o que o Responsavel precisa
 * ver para decidir, sem expor dados sensiveis do solicitante (somente nome e email).
 */
public final class AprovacaoDto {

    private AprovacaoDto() {
    }

    public record ItemFila(
            Long reservaId,
            Instant solicitadaEm,
            String solicitanteNome,
            String solicitanteEmail,
            Long salaId,
            String salaNome,
            Long professorId,
            String professorNome,
            Instant inicio,
            Instant termino,
            String finalidade,
            SituacaoPendencia aprovacaoRecurso,
            SituacaoPendencia validacaoDocente,
            List<MaterialNaFila> materiais) {

        public static ItemFila de(Reservation reserva) {
            Professor professor = reserva.getProfessor();
            return new ItemFila(
                    reserva.getId(),
                    reserva.getCriadoEm(),
                    reserva.getSolicitante().getName(),
                    reserva.getSolicitante().getEmail(),
                    reserva.getSala().getId(),
                    reserva.getSala().getNome(),
                    professor == null ? null : professor.getId(),
                    professor == null ? null : professor.getNome(),
                    reserva.getInicio(),
                    reserva.getTermino(),
                    reserva.getFinalidade(),
                    reserva.getAprovacaoRecurso(),
                    reserva.getValidacaoDocente(),
                    reserva.getMateriais().stream().map(MaterialNaFila::de).toList());
        }
    }

    public record MaterialNaFila(Long materialId, String nome, int quantidade, boolean restrito) {

        static MaterialNaFila de(ReservationMaterial item) {
            return new MaterialNaFila(
                    item.getMaterial().getId(),
                    item.getMaterial().getNome(),
                    item.getQuantidade(),
                    item.getMaterial().isRestrito());
        }
    }

    /** Corpo da rejeicao (APR-02, #34): o motivo e obrigatorio e fica preservado na reserva. */
    public record Rejeicao(
            @NotBlank(message = "motivo e obrigatorio")
            @Size(max = 500, message = "motivo deve ter no maximo 500 caracteres") String motivo) {
    }
}
