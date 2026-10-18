package com.avisa.repository;

import com.avisa.entity.AvisEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositori Spring Data JPA per a la gestió d'avisos i notificacions a PostgreSQL.
 */
@Repository
public interface AvisRepository extends JpaRepository<AvisEntity, String> {
	List<AvisEntity> findByUsuariIdOrderByDataDescCreatedAtDesc(String usuariId);
	List<AvisEntity> findByUsuariIdAndLlegitFalse(String usuariId);
}

