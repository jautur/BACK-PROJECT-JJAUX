package com.avisa.repository;

import com.avisa.entity.PressupostEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositori Spring Data JPA per a la gestió de pressupostos a PostgreSQL.
 */
@Repository
public interface PressupostRepository extends JpaRepository<PressupostEntity, String> {
	List<PressupostEntity> findByIncidenciaId(String incidenciaId);
	List<PressupostEntity> findByProfessionalId(String professionalId);
}

