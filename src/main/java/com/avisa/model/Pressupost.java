package com.avisa.model;

import java.time.LocalDateTime;

/**
 * Model de dades per a un Pressupost enviat per un professional per a una incidència de client.
 * - Registra la proposta econòmica concreta.
 * - En ser acceptat, genera transaccionalment la tasca i la incidència s'elimina de les publicacions.
 * - Si es rebutja, s'elimina sense generar cap tasca.
 */
public record Pressupost(
    String id,
    String incidenciaId,
    String incidenciaTitol,
    String professionalId,
    String professionalNom,
    String preu,
    String descripcio,
    String duracioEstimada,
    LocalDateTime dataCreacio
) {
    public Pressupost(
        String id,
        String incidenciaId,
        String professionalId,
        String professionalNom,
        String preu,
        String descripcio,
        String duracioEstimada,
        LocalDateTime dataCreacio
    ) {
        this(id, incidenciaId, null, professionalId, professionalNom, preu, descripcio, duracioEstimada, dataCreacio);
    }
}

