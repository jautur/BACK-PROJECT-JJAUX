package com.avisa.dto;

/**
 * DTO per a la petició de registre d'un nou compte d'usuari a JJAUX.
 * Els clients necessiten correu i contrasenya per publicar tasques.
 * Els professionals, com a fills del client, hereten les funcions de client i
 * requereixen a més un número de telèfon per resoldre peticions i presentar ofertes.
 *
 * @param nom Nom visible complet de l'usuari
 * @param email Correu electrònic principal per a l'inici de sessió
 * @param password Contrasenya de seguretat
 * @param rol Rol seleccionat ('client' o 'professional')
 * @param telefon Telèfon de contacte (requerit per a professionals)
 */
public record RegisterRequest(
	String nom,
	String email,
	String password,
	String rol,
	String telefon
) {
}
