package br.com.senac.gerenciadordesalas.salas;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SalaRepository extends JpaRepository<Sala, Long>, JpaSpecificationExecutor<Sala> {
    
    boolean existsByNome(String nome);

    boolean existsByNomeAndIdNot(String nome, Long id);
    
}