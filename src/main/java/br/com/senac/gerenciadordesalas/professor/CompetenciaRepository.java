package br.com.senac.gerenciadordesalas.professor;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CompetenciaRepository extends JpaRepository<Competencia, Long> {
    Optional<Competencia> findByNome(String nome);
}