package com.avisa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entitat JPA que mapeja la taula d'usuaris a PostgreSQL mitjançant anotacions '@'.
 */
@Entity
@Table(name = "usuaris")
public class UsuariEntity {

	@Id
	@Column(name = "id", length = 100, nullable = false)
	private String id;

	@Column(name = "nom", length = 150, nullable = false)
	private String nom;

	@Column(name = "email", length = 150, nullable = false, unique = true)
	private String email;

	@Column(name = "password", length = 255, nullable = false)
	private String password;

	@Column(name = "rol", length = 50, nullable = false)
	private String rol;

	@Column(name = "telefon", length = 50)
	private String telefon;

	@Column(name = "bio", columnDefinition = "TEXT")
	private String bio;

	@Column(name = "percentil", columnDefinition = "INT DEFAULT 3")
	private Integer percentil = 3;

	@Column(name = "verificat")
	private Boolean verificat = false;

	@Column(name = "sollicitud_verificacio")
	private Boolean sollicitudVerificacio = false;

	@Column(name = "foto", columnDefinition = "TEXT")
	private String foto;

	@Column(name = "foto_perfil", columnDefinition = "TEXT")
	private String fotoPerfil;

	@Column(name = "logo", columnDefinition = "TEXT")
	private String logo;

	@Column(name = "created_at")
	private LocalDateTime createdAt = LocalDateTime.now();

	public UsuariEntity() {}

	public UsuariEntity(String id, String nom, String email, String password, String rol, String telefon, String bio, Integer percentil) {
		this(id, nom, email, password, rol, telefon, bio, percentil, false, false);
	}

	public UsuariEntity(String id, String nom, String email, String password, String rol, String telefon, String bio, Integer percentil, Boolean verificat, Boolean sollicitudVerificacio) {
		this.id = id;
		this.nom = nom;
		this.email = email;
		this.password = password;
		this.rol = rol;
		this.telefon = telefon;
		this.bio = bio;
		this.percentil = (percentil != null) ? percentil : 3;
		this.verificat = (verificat != null) ? verificat : false;
		this.sollicitudVerificacio = (sollicitudVerificacio != null) ? sollicitudVerificacio : false;
		this.createdAt = LocalDateTime.now();
	}

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public String getNom() { return nom; }
	public void setNom(String nom) { this.nom = nom; }

	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }

	public String getPassword() { return password; }
	public void setPassword(String password) { this.password = password; }

	public String getRol() { return rol; }
	public void setRol(String rol) { this.rol = rol; }

	public String getTelefon() { return telefon; }
	public void setTelefon(String telefon) { this.telefon = telefon; }

	public String getBio() { return bio; }
	public void setBio(String bio) { this.bio = bio; }

	public Integer getPercentil() { return percentil; }
	public void setPercentil(Integer percentil) { this.percentil = percentil; }

	public Boolean getVerificat() { return verificat; }
	public void setVerificat(Boolean verificat) { this.verificat = verificat; }

	public Boolean getSollicitudVerificacio() { return sollicitudVerificacio; }
	public void setSollicitudVerificacio(Boolean sollicitudVerificacio) { this.sollicitudVerificacio = sollicitudVerificacio; }

	public String getFoto() { return foto; }
	public void setFoto(String foto) { this.foto = foto; }

	public String getFotoPerfil() { return fotoPerfil; }
	public void setFotoPerfil(String fotoPerfil) { this.fotoPerfil = fotoPerfil; }

	public String getLogo() { return logo; }
	public void setLogo(String logo) { this.logo = logo; }

	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

