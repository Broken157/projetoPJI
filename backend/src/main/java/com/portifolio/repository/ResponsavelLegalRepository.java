package com.portifolio.repository;

import com.portifolio.model.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResponsavelLegalRepository extends JpaRepository<ResponsavelLegal, Long> {
    @Query("select r from ResponsavelLegal r where r.tokenConsentimento like concat(:prefixo, '%')")
    Optional<ResponsavelLegal> findByTokenPrefixo(@Param("prefixo") String prefixo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ResponsavelLegal r where r.tokenConsentimento like concat(:prefixo, '%')")
    Optional<ResponsavelLegal> findByTokenPrefixoForUpdate(@Param("prefixo") String prefixo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ResponsavelLegal r join r.usuario u where lower(u.email) = lower(:email)")
    Optional<ResponsavelLegal> findByUsuarioEmailForUpdate(@Param("email") String email);
}
