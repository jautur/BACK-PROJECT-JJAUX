package com.avisa.model;

import java.time.LocalDateTime;

/**
 * Model de dades per a una Ressenya / Valoració (resenyes).
 * Permet que un Client valore la feina realitzada per un Treballador/Professional.
 *
 * @param id Identificador únic de la ressenya (p. ex. R-001)
 * @param clientId Identificador de l'usuari client que emet la valoració (FK a usuaris)
 * @param treballadorId Identificador de l'usuari treballador/professional valorat (FK a usuaris)
 * @param tascaId Identificador opcional de la tasca associada (FK a tasques)
 * @param puntuacio Puntuació numèrica de 1 a 5 punts
 * @param descripcio Comentari o text descriptiu de la valoració
 * @param createdAt Data i hora de publicació de la ressenya
 */
public record Resenya(
	String id,
	String clientId,
	String treballadorId,
	String tascaId,
	int puntuacio,
	String descripcio,
	LocalDateTime createdAt
) {
}
