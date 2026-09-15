package com.portifolio.repository;

import com.portifolio.model.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Override
    Optional<Usuario> findById(Long id);

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    Optional<Usuario> findByTokenRecuperacao(String tokenRecuperacao);

    // RF32: busca por conta Google vinculada
    Optional<Usuario> findByGoogleId(String googleId);
}
