package com.avisa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entitat JPA que mapeja la taula d'incidències de clients a PostgreSQL.
 */
@Entity
@Table(name = "incidencies")
public class IncidenciaEntity {

	@Id
	@Column(name = "id", length = 100, nullable = false)
	private String id;

	@Column(name = "client_id", length = 100)
	private String clientId;

	@Column(name = "client_nom", length = 150, nullable = false)
	private String clientNom;

	@Column(name = "titol", length = 255, nullable = false)
	private String titol;

	@Column(name = "descripcio", columnDefinition = "TEXT")
	private String descripcio;

	@Column(name = "categoria", length = 100)
	private String categoria;

	@Column(name = "budget", length = 50)
	private String budget;

	@Column(name = "estat", length = 50)
	private String estat = "Oberta";

	@Column(name = "created_at")
	private LocalDateTime createdAt = LocalDateTime.now();

	public IncidenciaEntity() {}

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public String getClientId() { return clientId; }
	public void setClientId(String clientId) { this.clientId = clientId; }

	public String getClientNom() { return clientNom; }
	public void setClientNom(String clientNom) { this.clientNom = clientNom; }

	public String getTitol() { return titol; }
	public void setTitol(String titol) { this.titol = titol; }

	public String getDescripcio() { return descripcio; }
	public void setDescripcio(String descripcio) { this.descripcio = descripcio; }

	public String getCategoria() { return categoria; }
	public void setCategoria(String categoria) { this.categoria = categoria; }

	public String getBudget() { return budget; }
	public void setBudget(String budget) { this.budget = budget; }

	public String getEstat() { return estat; }
	public void setEstat(String estat) { this.estat = estat; }

	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

