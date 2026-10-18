package com.avisa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entitat JPA que mapeja la taula de publicacions (ofertes i incidències) a PostgreSQL.
 */
@Entity
@Table(name = "publicacions")
public class PublicacioEntity {

	@Id
	@Column(name = "id", length = 100, nullable = false)
	private String id;

	@Column(name = "usuari_id", length = 100)
	private String usuariId;

	@Column(name = "usuari_nom", length = 150, nullable = false)
	private String usuariNom;

	@Column(name = "tipus", length = 50, nullable = false)
	private String tipus;

	@Column(name = "titol", length = 255, nullable = false)
	private String titol;

	@Column(name = "descripcio", columnDefinition = "TEXT")
	private String descripcio;

	@Column(name = "categoria", length = 100)
	private String categoria;

	@Column(name = "preu_o_pressupost", length = 50)
	private String preuOPressupost;

	@Column(name = "created_at")
	private LocalDateTime createdAt = LocalDateTime.now();

	public PublicacioEntity() {}

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public String getUsuariId() { return usuariId; }
	public void setUsuariId(String usuariId) { this.usuariId = usuariId; }

	public String getUsuariNom() { return usuariNom; }
	public void setUsuariNom(String usuariNom) { this.usuariNom = usuariNom; }

	public String getTipus() { return tipus; }
	public void setTipus(String tipus) { this.tipus = tipus; }

	public String getTitol() { return titol; }
	public void setTitol(String titol) { this.titol = titol; }

	public String getDescripcio() { return descripcio; }
	public void setDescripcio(String descripcio) { this.descripcio = descripcio; }

	public String getCategoria() { return categoria; }
	public void setCategoria(String categoria) { this.categoria = categoria; }

	public String getPreuOPressupost() { return preuOPressupost; }
	public void setPreuOPressupost(String preuOPressupost) { this.preuOPressupost = preuOPressupost; }

	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

