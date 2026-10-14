package com.avisa.model;

/**
 * Model de dades per a Incidències i Tasques a la plataforma AVISA.
 * - Incidència: Petició oberta per un client (té client, professional = null, tipus = 'incidencia', estat = 'Oberta').
 * - Tasca: Treball en curs on convergeixen un client i un professional obligatoris (té client, professional, tipus = 'tasca', estat = 'En curs' o 'Finalitzada').
 */
public record Tasca(
    String id,
    String titol,
    String descripcio,
    String categoria,
    String client,
    String professional,
    String budget,
    String estat,
    String tipus
) {
    public Tasca(String id, String titol, String descripcio, String categoria, String client, String budget, String estat) {
        this(
            id,
            titol,
            descripcio,
            categoria,
            client,
            null,
            budget,
            estat,
            (estat != null && ("En curs".equalsIgnoreCase(estat) || "Finalitzada".equalsIgnoreCase(estat))) ? "tasca" : "incidencia"
        );
    }
}
