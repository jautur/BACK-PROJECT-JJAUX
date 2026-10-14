package com.avisa.model;

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
 */
public record Usuari(
	String id,
	String nom,
	String email,
	String rol,
	String telefon,
	String bio
) {
}