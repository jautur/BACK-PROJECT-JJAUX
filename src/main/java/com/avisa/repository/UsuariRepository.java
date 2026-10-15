package com.avisa.repository;

import com.avisa.entity.UsuariEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositori Spring Data JPA per a la gestió d'usuaris a PostgreSQL.
 */
@Repository
public interface UsuariRepository extends JpaRepository<UsuariEntity, String> {
	Optional<UsuariEntity> findByEmailIgnoreCase(String email);
	List<UsuariEntity> findByRolIgnoreCase(String rol);
}

