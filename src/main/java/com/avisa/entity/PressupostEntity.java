package com.avisa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entitat JPA que mapeja la taula de pressupostos a PostgreSQL.
 */
@Entity
@Table(name = "pressupostos")
public class PressupostEntity {

	@Id
	@Column(name = "id", length = 100, nullable = false)
	private String id;

	@Column(name = "incidencia_id", length = 100)
	private String incidenciaId;

	@Column(name = "professional_id", length = 100)
	private String professionalId;

	@Column(name = "professional_nom", length = 150, nullable = false)
	private String professionalNom;

	@Column(name = "preu", length = 50, nullable = false)
	private String preu;

	@Column(name = "descripcio", columnDefinition = "TEXT")
	private String descripcio;

	@Column(name = "duracio_estimada", length = 100)
	private String duracioEstimada;

	@Column(name = "data_creacio")
	private LocalDateTime dataCreacio = LocalDateTime.now();

	public PressupostEntity() {}

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public String getIncidenciaId() { return incidenciaId; }
	public void setIncidenciaId(String incidenciaId) { this.incidenciaId = incidenciaId; }

	public String getProfessionalId() { return professionalId; }
	public void setProfessionalId(String professionalId) { this.professionalId = professionalId; }

	public String getProfessionalNom() { return professionalNom; }
	public void setProfessionalNom(String professionalNom) { this.professionalNom = professionalNom; }

	public String getPreu() { return preu; }
	public void setPreu(String preu) { this.preu = preu; }

	public String getDescripcio() { return descripcio; }
	public void setDescripcio(String descripcio) { this.descripcio = descripcio; }

	public String getDuracioEstimada() { return duracioEstimada; }
	public void setDuracioEstimada(String duracioEstimada) { this.duracioEstimada = duracioEstimada; }

	public LocalDateTime getDataCreacio() { return dataCreacio; }
	public void setDataCreacio(LocalDateTime dataCreacio) { this.dataCreacio = dataCreacio; }
}

