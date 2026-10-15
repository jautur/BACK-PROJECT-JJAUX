package com.avisa.repository;

import com.avisa.entity.PublicacioEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositori Spring Data JPA per a la gestió de publicacions a PostgreSQL.
 */
@Repository
public interface PublicacioRepository extends JpaRepository<PublicacioEntity, String> {
	List<PublicacioEntity> findByTipusIgnoreCase(String tipus);
	List<PublicacioEntity> findByUsuariId(String usuariId);
}

