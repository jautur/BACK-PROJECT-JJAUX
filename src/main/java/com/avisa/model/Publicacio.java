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
    String usuariId,
    String usuariNom,
    String tipus,
    String titol,
    String descripcio,
    String categoria,
    String preuOPressupost,
    LocalDateTime createdAt
) {
    @JsonProperty("professional")
    public String professional() {
        return "OFERTA".equalsIgnoreCase(tipus) ? usuariNom : null;
    }

    @JsonProperty("client")
    public String client() {
        return "INCIDENCIA".equalsIgnoreCase(tipus) ? usuariNom : null;
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

