package com.avisa.repository;

import com.avisa.entity.InteresEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositori Spring Data JPA per a la gestió d'interessos a PostgreSQL.
 */
@Repository
public interface InteresRepository extends JpaRepository<InteresEntity, String> {
	List<InteresEntity> findByPublicacioId(String publicacioId);
	List<InteresEntity> findByClientId(String clientId);
}

