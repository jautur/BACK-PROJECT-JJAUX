package com.avisa.repository;

import com.avisa.entity.TascaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositori Spring Data JPA per a la gestió de tasques a PostgreSQL.
 */
@Repository
public interface TascaRepository extends JpaRepository<TascaEntity, String> {
	List<TascaEntity> findByClientId(String clientId);
	List<TascaEntity> findByProfessionalId(String professionalId);
	List<TascaEntity> findByEstat(String estat);
}

