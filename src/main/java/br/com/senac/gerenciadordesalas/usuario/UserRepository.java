package br.com.senac.gerenciadordesalas.usuario;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

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

    Page<User> findAllBy(Pageable pagina);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.profile = :perfil and u.active = true order by u.id")
    List<User> bloquearAtivosDoPerfil(@Param("perfil") Role perfil);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> bloquearPorId(@Param("id") Long id);
}
