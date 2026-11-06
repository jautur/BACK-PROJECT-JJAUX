package com.avisa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Petició de creació o actualització d'una incidència per part d'un client.
 */
public record IncidenciaRequest(
    String clientId,
    String clientNom,
    String client,
    @NotBlank(message = "El títol és obligatori") @Size(max = 255, message = "El títol pot tenir com a màxim 255 caràcters") @Pattern(regexp = "^[^<>]*$", message = "No pot contenir els caràcters < ni >")
    String titol,
    @Size(max = 5000, message = "La descripció pot tenir com a màxim 5000 caràcters")
    String descripcio,
    @Size(max = 100, message = "La categoria pot tenir com a màxim 100 caràcters")
    String categoria,
    @Size(max = 100, message = "El pressupost pot tenir com a màxim 100 caràcters")
    String budget,
    String estat
) {
    public IncidenciaRequest ambClient(String id) {
        return new IncidenciaRequest(id, null, id, titol, descripcio, categoria, budget, estat);
    }

    public String resolveClient() {
        if (client != null && !client.isBlank()) return client;
        if (clientNom != null && !clientNom.isBlank()) return clientNom;
        return clientId;
    }
}

