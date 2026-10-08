package com.portifolio.repository;

import com.portifolio.model.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByUsername(String username);
    boolean existsByCpf(String cpf);
    boolean existsByCnpj(String cnpj);

    @Override
    @EntityGraph(attributePaths = "responsaveisLegais")
    Optional<Usuario> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = "responsaveisLegais")
    Optional<Usuario> findByEmail(String email);

    @EntityGraph(attributePaths = "responsaveisLegais")
    Optional<Usuario> findByEmailIgnoreCase(String email);

    Optional<Usuario> findByTokenRecuperacao(String tokenRecuperacao);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.tokenVerificacao = :hash")
    Optional<Usuario> findByTokenVerificacaoForUpdate(@Param("hash") String hash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where lower(u.email) = lower(:email)")
    Optional<Usuario> findByEmailIgnoreCaseForUpdate(@Param("email") String email);

    // RF32: busca por conta Google vinculada
    Optional<Usuario> findByGoogleId(String googleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.googleId = :googleId")
    Optional<Usuario> findByGoogleIdForUpdate(@Param("googleId") String googleId);
}
