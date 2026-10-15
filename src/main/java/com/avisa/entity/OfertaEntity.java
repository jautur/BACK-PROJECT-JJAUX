package com.avisa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entitat JPA que mapeja la taula d'ofertes de serveis a PostgreSQL.
 */
@Entity
@Table(name = "ofertes")
public class OfertaEntity {

	@Id
	@Column(name = "id", length = 100, nullable = false)
	private String id;

	@Column(name = "tasca_id", length = 100)
	private String tascaId;

	@Column(name = "incidencia_id", length = 100)
	private String incidenciaId;

	@Column(name = "professional", length = 150, nullable = false)
	private String professional;

	@Column(name = "categoria", length = 100)
	private String categoria;

	@Column(name = "price", length = 50, nullable = false)
	private String price;

	@Column(name = "note", columnDefinition = "TEXT")
	private String note;

	@Column(name = "estat", length = 50)
	private String estat = "Pendent";

	@Column(name = "created_at")
	private LocalDateTime createdAt = LocalDateTime.now();

	public OfertaEntity() {}

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public String getTascaId() { return tascaId; }
	public void setTascaId(String tascaId) { this.tascaId = tascaId; }

	public String getIncidenciaId() { return incidenciaId; }
	public void setIncidenciaId(String incidenciaId) { this.incidenciaId = incidenciaId; }

	public String getProfessional() { return professional; }
	public void setProfessional(String professional) { this.professional = professional; }

	public String getCategoria() { return categoria; }
	public void setCategoria(String categoria) { this.categoria = categoria; }

	public String getPrice() { return price; }
	public void setPrice(String price) { this.price = price; }

	public String getNote() { return note; }
	public void setNote(String note) { this.note = note; }

	public String getEstat() { return estat; }
	public void setEstat(String estat) { this.estat = estat; }

	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

