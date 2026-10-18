package com.avisa.repository;

import com.avisa.entity.ResenyaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositori Spring Data JPA per a la gestió de ressenyes a PostgreSQL.
 */
@Repository
public interface ResenyaRepository extends JpaRepository<ResenyaEntity, String> {
	List<ResenyaEntity> findByProfessionalNomIgnoreCase(String professionalNom);
}

