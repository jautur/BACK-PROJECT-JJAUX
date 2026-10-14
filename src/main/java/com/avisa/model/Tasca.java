package com.avisa.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model de dades per a Incidències i Tasques a la plataforma AVISA.
 * - Incidència: Petició oberta per un client (té client, professional = null, tipus = 'incidencia', estat = 'Oberta').
 * - Oferta: Servei proposat per un professional (té professional, client = null, tipus = 'oferta').
 * - Tasca: Treball en curs on convergeixen un client i un professional obligatoris (tipus = 'tasca', estat = 'En curs' o 'Finalitzada').
 * En finalitzar, s'enregistren directament a la tasca la valoracio (1 a 5) i l'opinio del client.
 */
public record Tasca(
    String id,
    String titol,
    String descripcio,
    String categoria,
    String clientId,
    String clientNom,
    String professionalId,
    String professionalNom,
    String origenId,
    String budget,
    String estat,
    String tipus,
    Integer valoracio,
    String opinio
) {
    @JsonProperty("client")
    public String client() {
        return clientNom != null ? clientNom : clientId;
    }

    @JsonProperty("professional")
    public String professional() {
        return professionalNom != null ? professionalNom : professionalId;
    }

    public Tasca(
        String id,
        String titol,
        String descripcio,
        String categoria,
        String clientNom,
        String professionalNom,
        String budget,
        String estat,
        String tipus
    ) {
        this(
            id,
            titol,
            descripcio,
            categoria,
            null,
            clientNom,
            null,
            professionalNom,
            null,
            budget,
            estat,
            tipus,
            null,
            null
        );
    }

    public Tasca(String id, String titol, String descripcio, String categoria, String client, String budget, String estat) {
        this(
            id,
            titol,
            descripcio,
            categoria,
            null,
            client,
            null,
            null,
            null,
            budget,
            estat,
            (estat != null && ("En curs".equalsIgnoreCase(estat) || "Finalitzada".equalsIgnoreCase(estat))) ? "tasca" : "incidencia",
            null,
            null
        );
    }
}
