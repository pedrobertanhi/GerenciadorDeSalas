package br.com.senac.gerenciadordesalas.usuario;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório de usuários sem operações de exclusão física.
 * Usuários são inativados com User#deactivate e permanecem no histórico (USR-04).
 */
public interface UserRepository extends Repository<User, Long> {

    <S extends User> S save(S usuario);

    <S extends User> S saveAndFlush(S usuario);

    Optional<User> findById(Long id);

    List<User> findAll();

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}