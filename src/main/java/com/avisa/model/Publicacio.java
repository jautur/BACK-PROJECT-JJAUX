package com.avisa.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * Model de dades per a una Publicació a la plataforma AVISA.
 * Diferencia entre dos tipus exclusius:
 * - 'OFERTA': Servei posat a disposició pels professionals (permanent, pot rebre múltiples interessos de diferents clients).
 * - 'INCIDENCIA': Necessitat o avaria publicada per un client (rebrà pressupostos i s'elimina en ser contractada).
 */
public record Publicacio(
    String id,
    String autorId,
    String autorNom,
    String tipus,
    String titol,
    String descripcio,
    String categoria,
    String preuOPressupost,
    LocalDateTime createdAt,
    String estat
) {
    public Publicacio(String id, String autorId, String autorNom, String tipus, String titol, String descripcio, String categoria, String preuOPressupost, LocalDateTime createdAt) {
        this(id, autorId, autorNom, tipus, titol, descripcio, categoria, preuOPressupost, createdAt, null);
    }

    public Publicacio ambAutor(String autorId) {
        return new Publicacio(id, autorId, null, tipus, titol, descripcio, categoria, preuOPressupost, createdAt, estat);
    }

    @JsonProperty("professional")
    public String professional() {
        return "OFERTA".equalsIgnoreCase(tipus) ? autorNom : null;
    }

    @JsonProperty("client")
    public String client() {
        return "INCIDENCIA".equalsIgnoreCase(tipus) ? autorNom : null;
    }

    @JsonProperty("price")
    public String price() {
        return preuOPressupost;
    }

    @JsonProperty("budget")
    public String budget() {
        return preuOPressupost;
    }
}

