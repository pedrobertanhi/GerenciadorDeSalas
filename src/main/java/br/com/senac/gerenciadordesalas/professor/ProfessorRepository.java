package br.com.senac.gerenciadordesalas.professor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
    @Query("select distinct p from Professor p left join fetch p.competencias where p.ativo = true and (:competencia is null or exists (select c.id from Professor p2 join p2.competencias c where p2 = p and lower(c.nome) = :competencia))")
    List<Professor> buscarAtivos(@Param("competencia") String competencia);
}