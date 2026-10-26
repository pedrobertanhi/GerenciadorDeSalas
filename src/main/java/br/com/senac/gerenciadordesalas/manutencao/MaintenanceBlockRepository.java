package br.com.senac.gerenciadordesalas.manutencao;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositório sem operações de exclusão: bloqueios de manutenção são histórico e nunca
 * são apagados. O encerramento acontece pelo estado (CONCLUIDO ou CANCELADO).
 */
public interface MaintenanceBlockRepository extends Repository<MaintenanceBlock, Long> {

    <S extends MaintenanceBlock> S saveAndFlush(S bloqueio);

    Optional<MaintenanceBlock> findById(Long id);

    @Query("""
            select b from MaintenanceBlock b
             where (:recursoTipo is null or b.recursoTipo = :recursoTipo)
               and (:recursoId is null or b.recursoId = :recursoId)
               and (:status is null or b.status = :status)
             order by b.inicio desc, b.id desc
            """)
    List<MaintenanceBlock> buscar(
            @Param("recursoTipo") TipoRecurso recursoTipo,
            @Param("recursoId") Long recursoId,
            @Param("status") StatusBloqueio status);
}