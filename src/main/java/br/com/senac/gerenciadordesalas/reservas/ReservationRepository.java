package br.com.senac.gerenciadordesalas.reservas;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Acesso a persistencia do agregado {@link Reservation} (RSV-01, #23).
 * Metodos de consulta usados pelos casos de uso de criacao e deteccao de conflito.
 */
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findBySolicitanteIdOrderByInicioDesc(Long solicitanteId);

    List<Reservation> findBySalaIdOrderByInicioAsc(Long salaId);

    /**
     * Reservas da sala cujo periodo semiaberto [inicio, termino) sobrepoe
     * o periodo informado. Status terminais (REJEITADA, CANCELADA, CONCLUIDA)
     * sao excluidos.
     */
    @Query("""
            SELECT r FROM Reservation r
            WHERE r.sala.id = :salaId
              AND r.status NOT IN (
                  br.com.senac.gerenciadordesalas.reservas.StatusReserva.REJEITADA,
                  br.com.senac.gerenciadordesalas.reservas.StatusReserva.CANCELADA,
                  br.com.senac.gerenciadordesalas.reservas.StatusReserva.CONCLUIDA,
                  br.com.senac.gerenciadordesalas.reservas.StatusReserva.NAO_COMPARECEU)
              AND r.inicio < :termino
              AND r.termino > :inicio
            """)
    List<Reservation> buscarAtivasDaSalaSobrepondo(
            @Param("salaId") Long salaId,
            @Param("inicio") Instant inicio,
            @Param("termino") Instant termino);

    /** Mesma logica para o professor, usada na deteccao de conflito docente (RSV-07). */
    @Query("""
            SELECT r FROM Reservation r
            WHERE r.professor.id = :professorId
              AND r.status NOT IN (
                  br.com.senac.gerenciadordesalas.reservas.StatusReserva.REJEITADA,
                  br.com.senac.gerenciadordesalas.reservas.StatusReserva.CANCELADA,
                  br.com.senac.gerenciadordesalas.reservas.StatusReserva.CONCLUIDA,
                  br.com.senac.gerenciadordesalas.reservas.StatusReserva.NAO_COMPARECEU)
              AND r.inicio < :termino
              AND r.termino > :inicio
            """)
    List<Reservation> buscarAtivasDoProfessorSobrepondo(
            @Param("professorId") Long professorId,
            @Param("inicio") Instant inicio,
            @Param("termino") Instant termino);

    /**
     * Soma as quantidades do material comprometidas por reservas ativas no
     * período semiaberto informado. A reserva ignorada permite validar uma
     * alteração sem contabilizar seus próprios itens.
     */
    @Query("""
            SELECT COALESCE(SUM(rm.quantidade), 0) FROM ReservationMaterial rm
            WHERE rm.material.id = :materialId
              AND rm.reserva.status NOT IN (
                  br.com.senac.gerenciadordesalas.reservas.StatusReserva.REJEITADA,
                  br.com.senac.gerenciadordesalas.reservas.StatusReserva.CANCELADA,
                  br.com.senac.gerenciadordesalas.reservas.StatusReserva.CONCLUIDA,
                  br.com.senac.gerenciadordesalas.reservas.StatusReserva.NAO_COMPARECEU)
              AND rm.reserva.inicio < :termino
              AND rm.reserva.termino > :inicio
              AND (:reservaIgnorada IS NULL OR rm.reserva.id <> :reservaIgnorada)
            """)
    long quantidadeAtivaDoMaterialSobrepondo(
            @Param("materialId") Long materialId,
            @Param("inicio") Instant inicio,
            @Param("termino") Instant termino,
            @Param("reservaIgnorada") Long reservaIgnorada);

    @Query("""
            SELECT rm FROM ReservationMaterial rm JOIN FETCH rm.reserva r
            WHERE rm.material.id = :materialId
              AND r.status NOT IN (
                  br.com.senac.gerenciadordesalas.reservas.StatusReserva.REJEITADA,
                  br.com.senac.gerenciadordesalas.reservas.StatusReserva.CANCELADA,
                  br.com.senac.gerenciadordesalas.reservas.StatusReserva.CONCLUIDA,
                  br.com.senac.gerenciadordesalas.reservas.StatusReserva.NAO_COMPARECEU)
              AND r.inicio < :termino AND r.termino > :inicio
            """)
    List<ReservationMaterial> materiaisAtivosSobrepondo(
            @Param("materialId") Long materialId,
            @Param("inicio") Instant inicio,
            @Param("termino") Instant termino);
}
