package com.avisa.repository;

import com.avisa.entity.IncidenciaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositori Spring Data JPA per a la gestió d'incidències a PostgreSQL.
 */
@Repository
public interface IncidenciaRepository extends JpaRepository<IncidenciaEntity, String> {
	List<IncidenciaEntity> findByClientId(String clientId);
	List<IncidenciaEntity> findByEstatIgnoreCase(String estat);
}

