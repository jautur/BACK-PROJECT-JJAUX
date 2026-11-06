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
 * @param rol Rol del compte ('CLIENT', 'PROFESSIONAL', 'ADMIN')
 * @param telefon Telèfon de contacte opcional
 * @param bio Biografia o descripció d'habilitats
 * @param percentil Percentil de comissió reservat per a la plataforma (0 a 100, per defecte 3)
 * @param token Token JWT generat per OAuth2 Resource Server
 * @param verificat Indica si el professional està verificat oficialment per trucada
 * @param sollicitudVerificacio Indica si hi ha una sol·licitud de verificació pendent
 * @param fotoUrl Foto de perfil pròpia (/api/imatges/...), o null
 * @param logoUrl Logotip propi (/api/imatges/...), o null
 * @param emailVerificat Indica si l'usuari ha verificat la seva adreça d'email
 */
public record Usuari(
	String id,
	@jakarta.validation.constraints.Size(max = 150, message = "El nom pot tenir com a màxim 150 caràcters")
	@jakarta.validation.constraints.Pattern(regexp = "^[^<>]*$", message = "No pot contenir els caràcters < ni >")
	String nom,
	String email,
	String rol,
	@jakarta.validation.constraints.Pattern(regexp = "^$|^[0-9+()\\s.-]{6,20}$", message = "El telèfon només pot tenir xifres, espais, +, -, . i parèntesis (entre 6 i 20)")
	String telefon,
	@jakarta.validation.constraints.Size(max = 2000, message = "La bio pot tenir com a màxim 2000 caràcters")
	String bio,
	@Min(value = 0, message = "El percentil no pot ser inferior a 0")
	@Max(value = 100, message = "El percentil no pot superar 100")
	Integer percentil,
	String token,
	Boolean verificat,
	Boolean sollicitudVerificacio,
	String fotoUrl,
	String logoUrl,
	Boolean emailVerificat
) {
	public Usuari(String id, String nom, String email, String rol, String telefon, String bio, Integer percentil, String token, Boolean verificat, Boolean sollicitudVerificacio, String fotoUrl, String logoUrl) {
		this(id, nom, email, rol, telefon, bio, percentil, token, verificat, sollicitudVerificacio, fotoUrl, logoUrl, true);
	}

	public Usuari(String id, String nom, String email, String rol, String telefon, String bio, Integer percentil, String token, Boolean verificat, Boolean sollicitudVerificacio) {
		this(id, nom, email, rol, telefon, bio, percentil, token, verificat, sollicitudVerificacio, null, null, true);
	}

	public Usuari(String id, String nom, String email, String rol, String telefon, String bio, Integer percentil, String token) {
		this(id, nom, email, rol, telefon, bio, percentil, token, false, false, null, null, true);
	}

	public Usuari(String id, String nom, String email, String rol, String telefon, String bio, Integer percentil) {
		this(id, nom, email, rol, telefon, bio, percentil, null, false, false, null, null, true);
	}

	public Usuari(String id, String nom, String email, String rol, String telefon, String bio) {
		this(id, nom, email, rol, telefon, bio, 3, null, false, false, null, null, true);
	}

	public Usuari withToken(String token) {
		return new Usuari(id, nom, email, rol, telefon, bio, percentil, token, verificat, sollicitudVerificacio, fotoUrl, logoUrl, emailVerificat);
	}

	public Usuari withEmailVerificat(Boolean emailVerificat) {
		return new Usuari(id, nom, email, rol, telefon, bio, percentil, token, verificat, sollicitudVerificacio, fotoUrl, logoUrl, emailVerificat);
	}

	/** El que pot veure qualsevol: sense contacte, comissió ni estat intern de verificació. */
	public Usuari perfilPublic() {
		return new Usuari(id, nom, null, rol, null, bio, null, null, verificat, null, fotoUrl, logoUrl, emailVerificat);
	}

	/** El perfil públic més el contacte: per a l'altra part d'una tasca acceptada. */
	public Usuari perfilPublicAmbContacte() {
		return new Usuari(id, nom, email, rol, telefon, bio, null, null, verificat, null, fotoUrl, logoUrl, emailVerificat);
	}

}
