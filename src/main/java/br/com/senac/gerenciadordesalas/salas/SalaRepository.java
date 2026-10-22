package br.com.senac.gerenciadordesalas.salas;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SalaRepository extends JpaRepository<Sala, Long> {
    
    // Usado na criação para evitar nomes duplicados
    boolean existsByNome(String nome);
    
    // Usado na atualização para garantir que o novo nome não pertence a outra sala
    boolean existsByNomeAndIdNot(String nome, Long id);
}