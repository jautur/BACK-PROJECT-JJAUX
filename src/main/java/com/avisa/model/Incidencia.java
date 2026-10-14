package com.avisa.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * Model de dades per a una Incidència a la plataforma AVISA.
 * Les incidències són creades exclusivament per clients (demandes d'ajuda, avaries o feines a la llar).
 * No tenen professional assignat; un cop un client accepta el pressupost d'un professional,
 * l'oferta i la incidència convergeixen en una nova TASCA a la taula tasques.
 */
public record Incidencia(
    String id,
    String clientId,
    String clientNom,
    String titol,
    String descripcio,
    String categoria,
    String budget,
    String estat,
    LocalDateTime createdAt
) {
    @JsonProperty("client")
    public String client() {
        return clientNom != null ? clientNom : clientId;
    }

    public Incidencia(String id, String clientNom, String titol, String descripcio, String categoria, String budget, String estat) {
        this(id, null, clientNom, titol, descripcio, categoria, budget, estat, LocalDateTime.now());
    }
}
