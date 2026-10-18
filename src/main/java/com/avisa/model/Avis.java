package com.avisa.model;

import java.time.LocalDate;

/**
 * Model de dades per a un avís o notificació de sistema.
 * Registra missatges automàtics adreçats als usuaris (clients o professionals).
 *
 * @param id Identificador únic de l'avís
 * @param usuariId Identificador de l'usuari receptor
 * @param missatge Contingut textual de la notificació
 * @param llegit Indicador si l'avís ha estat llegit o continua pendent
 * @param data Data de creació de l'avís
 */
public record Avis(
	String id,
	String usuariId,
	String missatge,
	Boolean llegit,
	LocalDate data
) {
}

