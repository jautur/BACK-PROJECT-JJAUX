package com.avisa.dto;

/**
 * Petició de creació o actualització d'una incidència per part d'un client.
 */
public record IncidenciaRequest(
    String clientId,
    String clientNom,
    String client,
    String titol,
    String descripcio,
    String categoria,
    String budget,
    String estat
) {
    public String resolveClient() {
        if (client != null && !client.isBlank()) return client;
        if (clientNom != null && !clientNom.isBlank()) return clientNom;
        return clientId;
    }
}

