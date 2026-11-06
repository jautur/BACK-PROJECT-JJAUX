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
	String clientNom,
	String treballadorId,
	String treballadorNom,
	@jakarta.validation.constraints.NotBlank(message = "Cal indicar la tasca")
	String tascaId,
	@jakarta.validation.constraints.Min(value = 1, message = "La valoració mínima és 1 estrella")
	@jakarta.validation.constraints.Max(value = 5, message = "La valoració màxima és 5 estrelles")
	int puntuacio,
	@jakarta.validation.constraints.Size(max = 2000, message = "El comentari pot tenir com a màxim 2000 caràcters")
	String descripcio,
	LocalDateTime createdAt
) {
	public Resenya(
		String id,
		String clientId,
		String treballadorId,
		String tascaId,
		int puntuacio,
		String descripcio,
		LocalDateTime createdAt
	) {
		this(id, clientId, null, treballadorId, null, tascaId, puntuacio, descripcio, createdAt);
	}
}

