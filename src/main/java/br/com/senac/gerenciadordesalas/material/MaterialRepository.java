package br.com.senac.gerenciadordesalas.material;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    boolean existsByPatrimonio(String patrimonio);

    boolean existsByPatrimonioAndIdNot(String patrimonio, Long id);

    Optional<Material> findByIdAndAtivoTrue(Long id);

    @Query("""
            select m from Material m
             where m.ativo = true
               and (:tipo is null or m.tipo = :tipo)
               and (:restrito is null or m.restrito = :restrito)
             order by m.nome
            """)
    List<Material> buscarAtivos(
            @Param("tipo") MaterialTipo tipo,
            @Param("restrito") Boolean restrito);
}
