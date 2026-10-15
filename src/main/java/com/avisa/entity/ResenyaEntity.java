package com.avisa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entitat JPA que mapeja la taula de ressenyes i valoracions a PostgreSQL.
 */
@Entity
@Table(name = "resenyes")
public class ResenyaEntity {

	@Id
	@Column(name = "id", length = 100, nullable = false)
	private String id;

	@Column(name = "tasca_id", length = 100)
	private String tascaId;

	@Column(name = "professional_nom", length = 150)
	private String professionalNom;

	@Column(name = "client_nom", length = 150)
	private String clientNom;

	@Column(name = "puntuacio")
	private Integer puntuacio;

	@Column(name = "comentari", columnDefinition = "TEXT")
	private String comentari;

	@Column(name = "created_at")
	private LocalDateTime createdAt = LocalDateTime.now();

	public ResenyaEntity() {}

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public String getTascaId() { return tascaId; }
	public void setTascaId(String tascaId) { this.tascaId = tascaId; }

	public String getProfessionalNom() { return professionalNom; }
	public void setProfessionalNom(String professionalNom) { this.professionalNom = professionalNom; }

	public String getClientNom() { return clientNom; }
	public void setClientNom(String clientNom) { this.clientNom = clientNom; }

	public Integer getPuntuacio() { return puntuacio; }
	public void setPuntuacio(Integer puntuacio) { this.puntuacio = puntuacio; }

	public String getComentari() { return comentari; }
	public void setComentari(String comentari) { this.comentari = comentari; }

	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

