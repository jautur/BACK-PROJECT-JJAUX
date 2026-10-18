package com.avisa.repository;

import com.avisa.entity.OfertaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositori Spring Data JPA per a la gestió d'ofertes de serveis a PostgreSQL.
 */
@Repository
public interface OfertaRepository extends JpaRepository<OfertaEntity, String> {
	List<OfertaEntity> findByTascaId(String tascaId);
	List<OfertaEntity> findByProfessionalIgnoreCase(String professional);
}

