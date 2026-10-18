package com.avisa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entitat JPA que mapeja la taula de tasques acordades a PostgreSQL.
 */
@Entity
@Table(name = "tasques")
public class TascaEntity {

	@Id
	@Column(name = "id", length = 100, nullable = false)
	private String id;

	@Column(name = "titol", length = 255, nullable = false)
	private String titol;

	@Column(name = "descripcio", columnDefinition = "TEXT")
	private String descripcio;

	@Column(name = "categoria", length = 100)
	private String categoria;

	@Column(name = "client_id", length = 100)
	private String clientId;

	@Column(name = "client_nom", length = 150)
	private String clientNom;

	@Column(name = "professional_id", length = 100)
	private String professionalId;

	@Column(name = "professional_nom", length = 150)
	private String professionalNom;

	@Column(name = "publicacio_id", length = 100)
	private String publicacioId;

	@Column(name = "origen_id", length = 100)
	private String origenId;

	@Column(name = "interes_id", length = 100)
	private String interesId;

	@Column(name = "pressupost_id", length = 100)
	private String pressupostId;

	@Column(name = "preu_acordat", length = 50)
	private String preuAcordat;

	@Column(name = "budget", length = 50)
	private String budget;

	@Column(name = "estat", length = 50)
	private String estat = "En curs";

	@Column(name = "tipus", length = 50)
	private String tipus = "tasca";

	@Column(name = "data_inici")
	private LocalDateTime dataInici;

	@Column(name = "data_finalitzacio")
	private LocalDateTime dataFinalitzacio;

	@Column(name = "valoracio")
	private Integer valoracio;

	@Column(name = "opinio", columnDefinition = "TEXT")
	private String opinio;

	@Column(name = "data_valoracio")
	private LocalDateTime dataValoracio;

	@Column(name = "created_at")
	private LocalDateTime createdAt = LocalDateTime.now();

	public TascaEntity() {}

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public String getTitol() { return titol; }
	public void setTitol(String titol) { this.titol = titol; }

	public String getDescripcio() { return descripcio; }
	public void setDescripcio(String descripcio) { this.descripcio = descripcio; }

	public String getCategoria() { return categoria; }
	public void setCategoria(String categoria) { this.categoria = categoria; }

	public String getClientId() { return clientId; }
	public void setClientId(String clientId) { this.clientId = clientId; }

	public String getClientNom() { return clientNom; }
	public void setClientNom(String clientNom) { this.clientNom = clientNom; }

	public String getProfessionalId() { return professionalId; }
	public void setProfessionalId(String professionalId) { this.professionalId = professionalId; }

	public String getProfessionalNom() { return professionalNom; }
	public void setProfessionalNom(String professionalNom) { this.professionalNom = professionalNom; }

	public String getPublicacioId() { return publicacioId; }
	public void setPublicacioId(String publicacioId) { this.publicacioId = publicacioId; }

	public String getOrigenId() { return origenId; }
	public void setOrigenId(String origenId) { this.origenId = origenId; }

	public String getInteresId() { return interesId; }
	public void setInteresId(String interesId) { this.interesId = interesId; }

	public String getPressupostId() { return pressupostId; }
	public void setPressupostId(String pressupostId) { this.pressupostId = pressupostId; }

	public String getPreuAcordat() { return preuAcordat; }
	public void setPreuAcordat(String preuAcordat) { this.preuAcordat = preuAcordat; }

	public String getBudget() { return budget; }
	public void setBudget(String budget) { this.budget = budget; }

	public String getEstat() { return estat; }
	public void setEstat(String estat) { this.estat = estat; }

	public String getTipus() { return tipus; }
	public void setTipus(String tipus) { this.tipus = tipus; }

	public LocalDateTime getDataInici() { return dataInici; }
	public void setDataInici(LocalDateTime dataInici) { this.dataInici = dataInici; }

	public LocalDateTime getDataFinalitzacio() { return dataFinalitzacio; }
	public void setDataFinalitzacio(LocalDateTime dataFinalitzacio) { this.dataFinalitzacio = dataFinalitzacio; }

	public Integer getValoracio() { return valoracio; }
	public void setValoracio(Integer valoracio) { this.valoracio = valoracio; }

	public String getOpinio() { return opinio; }
	public void setOpinio(String opinio) { this.opinio = opinio; }

	public LocalDateTime getDataValoracio() { return dataValoracio; }
	public void setDataValoracio(LocalDateTime dataValoracio) { this.dataValoracio = dataValoracio; }

	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

