package com.avisa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entitat JPA que mapeja la taula d'avisos i notificacions a PostgreSQL.
 */
@Entity
@Table(name = "avisos")
public class AvisEntity {

	@Id
	@Column(name = "id", length = 100, nullable = false)
	private String id;

	@Column(name = "usuari_id", length = 100)
	private String usuariId;

	@Column(name = "missatge", columnDefinition = "TEXT", nullable = false)
	private String missatge;

	@Column(name = "llegit", nullable = false)
	private Boolean llegit = false;

	@Column(name = "enllac", length = 255)
	private String enllac;

	@Column(name = "data")
	private LocalDate data;

	@Column(name = "created_at")
	private LocalDateTime createdAt = LocalDateTime.now();

	public AvisEntity() {}

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public String getUsuariId() { return usuariId; }
	public void setUsuariId(String usuariId) { this.usuariId = usuariId; }

	public String getMissatge() { return missatge; }
	public void setMissatge(String missatge) { this.missatge = missatge; }

	public Boolean getLlegit() { return llegit; }
	public void setLlegit(Boolean llegit) { this.llegit = llegit; }

	public String getEnllac() { return enllac; }
	public void setEnllac(String enllac) { this.enllac = enllac; }

	public LocalDate getData() { return data; }
	public void setData(LocalDate data) { this.data = data; }

	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

