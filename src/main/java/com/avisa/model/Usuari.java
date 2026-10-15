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
	String token
) {
	public Usuari(String id, String nom, String email, String rol, String telefon, String bio, Integer percentil) {
		this(id, nom, email, rol, telefon, bio, percentil, null);
	}

	public Usuari(String id, String nom, String email, String rol, String telefon, String bio) {
		this(id, nom, email, rol, telefon, bio, 3, null);
	}

	public Usuari withToken(String token) {
		return new Usuari(id, nom, email, rol, telefon, bio, percentil, token);
	}
}