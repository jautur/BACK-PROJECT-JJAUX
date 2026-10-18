package com.avisa.model;

import java.time.LocalDateTime;

/**
 * Model de dades per a un Interès mostrat per un client en una oferta d'un professional.
 * - Registra la petició del client abans que s'acorde la feina.
 * - Si el professional accepta l'interès, es genera una única tasca vinculada (tascaId).
 * - Si es rebutja, s'elimina sense generar cap tasca.
 */
public record Interes(
    String id,
    String publicacioId,
    String publicacioTitol,
    String clientId,
    String clientNom,
    String professionalId,
    String professionalNom,
    String missatge,
    String tascaId,
    LocalDateTime dataCreacio
) {
    public Interes(
        String id,
        String publicacioId,
        String clientId,
        String clientNom,
        String missatge,
        String tascaId,
        LocalDateTime dataCreacio
    ) {
        this(id, publicacioId, null, clientId, clientNom, null, null, missatge, tascaId, dataCreacio);
    }
}

