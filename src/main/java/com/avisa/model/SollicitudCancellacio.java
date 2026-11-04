package com.avisa.model;

import java.time.LocalDateTime;

/**
 * Sol·licitud de cancel·lació d'una tasca i la seua resolució per part d'un administrador.
 * Es conserven totes perquè formen l'historial que l'administrador consulta en cas de negligència.
 */
public record SollicitudCancellacio(
    String id,
    String tascaId,
    String tascaTitol,
    String sollicitantId,
    String sollicitantNom,
    String motiu,
    String resolucio,
    String responsableId,
    String responsableNom,
    String adminId,
    LocalDateTime dataResolucio,
    LocalDateTime createdAt
) {
}
