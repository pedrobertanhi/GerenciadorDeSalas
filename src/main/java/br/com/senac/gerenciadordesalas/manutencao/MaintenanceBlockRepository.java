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

    /**
     * Verifica a indisponibilidade no intervalo semiaberto [inicio, fim). Bloqueios
     * cancelados não impedem uso; bloqueios concluídos valem somente até o encerramento.
     */
    @Query("""
            select count(b) > 0 from MaintenanceBlock b
             where b.recursoTipo = :recursoTipo
               and b.recursoId = :recursoId
               and b.inicio < :fim
               and (
                    (b.status = br.com.senac.gerenciadordesalas.manutencao.StatusBloqueio.ATIVO
                     and b.fim > :inicio)
                 or (b.status = br.com.senac.gerenciadordesalas.manutencao.StatusBloqueio.CONCLUIDO
                     and b.fim > :inicio
                     and b.encerradoEm > :inicio)
               )
            """)
    boolean existeBloqueioNoPeriodo(
            @Param("recursoTipo") TipoRecurso recursoTipo,
            @Param("recursoId") Long recursoId,
            @Param("inicio") java.time.Instant inicio,
            @Param("fim") java.time.Instant fim);
}
