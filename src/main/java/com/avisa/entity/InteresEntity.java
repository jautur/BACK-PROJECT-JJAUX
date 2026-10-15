package com.avisa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entitat JPA que mapeja la taula d'interessos a PostgreSQL.
 */
@Entity
@Table(name = "interessos")
public class InteresEntity {

	@Id
	@Column(name = "id", length = 100, nullable = false)
	private String id;

	@Column(name = "publicacio_id", length = 100)
	private String publicacioId;

	@Column(name = "client_id", length = 100)
	private String clientId;

	@Column(name = "client_nom", length = 150, nullable = false)
	private String clientNom;

	@Column(name = "missatge", columnDefinition = "TEXT")
	private String missatge;

	@Column(name = "tasca_id", length = 100)
	private String tascaId;

	@Column(name = "data_creacio")
	private LocalDateTime dataCreacio = LocalDateTime.now();

	public InteresEntity() {}

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public String getPublicacioId() { return publicacioId; }
	public void setPublicacioId(String publicacioId) { this.publicacioId = publicacioId; }

	public String getClientId() { return clientId; }
	public void setClientId(String clientId) { this.clientId = clientId; }

	public String getClientNom() { return clientNom; }
	public void setClientNom(String clientNom) { this.clientNom = clientNom; }

	public String getMissatge() { return missatge; }
	public void setMissatge(String missatge) { this.missatge = missatge; }

	public String getTascaId() { return tascaId; }
	public void setTascaId(String tascaId) { this.tascaId = tascaId; }

	public LocalDateTime getDataCreacio() { return dataCreacio; }
	public void setDataCreacio(LocalDateTime dataCreacio) { this.dataCreacio = dataCreacio; }
}

