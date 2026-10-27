package com.avisa.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Model de dades de l'Usuari en el Backend JJAUX.
 * Representa les dades públiques i de perfil d'un usuari registrat a la base de dades PostgreSQL (usuaris).
 *
 * @param id Identificador únic de l'usuari (ex: 'u-1', 'u-admin')
 * @param nom Nom visible o corporatiu de l'usuari
 * @param email Adreça de correu electrònic única utilitzada per a l'accés
 * @param rol Rol del compte ('client', 'professional', 'ambdos', 'admin')
 * @param telefon Telèfon de contacte opcional
 * @param bio Biografia o descripció d'habilitats
 * @param percentil Percentil de comissió reservat per a la plataforma (0 a 100, per defecte 3)
 * @param token Token JWT generat per OAuth2 Resource Server
 * @param verificat Indica si el professional està verificat oficialment per trucada
 * @param sollicitudVerificacio Indica si hi ha una sol·licitud de verificació pendent
 * @param foto URL o imatge de fons per a la targeta de recomanació del professional
 * @param fotoPerfil URL o isotip per a la foto de perfil corporativa
 * @param logo URL del logotip o fons distintiu de les targetes d'empresa
 */
public record Usuari(
	String id,
	String nom,
	String email,
	String rol,
	String telefon,
	String bio,
	@Min(value = 0, message = "El percentil no pot ser inferior a 0")
	@Max(value = 100, message = "El percentil no pot superar 100")
	Integer percentil,
	String token,
	Boolean verificat,
	Boolean sollicitudVerificacio,
	String foto,
	String fotoPerfil,
	String logo
) {
	public Usuari(String id, String nom, String email, String rol, String telefon, String bio, Integer percentil, String token, Boolean verificat, Boolean sollicitudVerificacio, String foto) {
		this(id, nom, email, rol, telefon, bio, percentil, token, verificat, sollicitudVerificacio, foto, null, foto);
	}

	public Usuari(String id, String nom, String email, String rol, String telefon, String bio, Integer percentil, String token, Boolean verificat, Boolean sollicitudVerificacio) {
		this(id, nom, email, rol, telefon, bio, percentil, token, verificat, sollicitudVerificacio, null, null, null);
	}

	public Usuari(String id, String nom, String email, String rol, String telefon, String bio, Integer percentil, String token) {
		this(id, nom, email, rol, telefon, bio, percentil, token, false, false, null, null, null);
	}

	public Usuari(String id, String nom, String email, String rol, String telefon, String bio, Integer percentil) {
		this(id, nom, email, rol, telefon, bio, percentil, null, false, false, null, null, null);
	}

	public Usuari(String id, String nom, String email, String rol, String telefon, String bio) {
		this(id, nom, email, rol, telefon, bio, 3, null, false, false, null, null, null);
	}

	public Usuari withToken(String token) {
		return new Usuari(id, nom, email, rol, telefon, bio, percentil, token, verificat, sollicitudVerificacio, foto, fotoPerfil, logo);
	}

	public Usuari withVerification(Boolean verificat, Boolean sollicitudVerificacio) {
		return new Usuari(id, nom, email, rol, telefon, bio, percentil, token, verificat, sollicitudVerificacio, foto, fotoPerfil, logo);
	}

	public Usuari withFoto(String foto) {
		return new Usuari(id, nom, email, rol, telefon, bio, percentil, token, verificat, sollicitudVerificacio, foto, fotoPerfil, foto);
	}
}
