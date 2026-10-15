package com.avisa.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * Model de dades per a una Tasca a la plataforma AVISA.
 * Una tasca representa un treball que ja ha sigut acordat entre un client i un professional.
 * Només es crea en dos casos:
 *   - CAS A: El professional accepta l'interés d'un client en una oferta.
 *   - CAS B: El client accepta un pressupost enviat per un professional per a una incidència.
 * 
 * Regla de cicle de vida (sense camp estat redundant a la BD):
 *   - dataFinalitzacio == null: tasca en curs.
 *   - dataFinalitzacio amb valor: tasca acabada / finalitzada.
 *   - valoracio i opinio: registrats directament a la tasca un cop finalitzada.
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
    String publicacioId,
    String interesId,
    String pressupostId,
    String preuAcordat,
    LocalDateTime dataInici,
    LocalDateTime dataFinalitzacio,
    Integer valoracio,
    String opinio,
    LocalDateTime dataValoracio
) {
    @JsonProperty("client")
    public String client() {
        return clientNom != null ? clientNom : clientId;
    }

    @JsonProperty("professional")
    public String professional() {
        return professionalNom != null ? professionalNom : professionalId;
    }

    @JsonProperty("budget")
    public String budget() {
        return preuAcordat != null ? preuAcordat : "";
    }

    @JsonProperty("estat")
    public String estat() {
        return dataFinalitzacio != null ? "Finalitzada" : "En curs";
    }

    @JsonProperty("tipus")
    public String tipus() {
        return "tasca";
    }

    public Tasca(
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
        this(
            id,
            titol,
            descripcio,
            categoria,
            clientId,
            clientNom,
            professionalId,
            professionalNom,
            origenId,
            null,
            null,
            budget,
            LocalDateTime.now(),
            ("Finalitzada".equalsIgnoreCase(estat)) ? LocalDateTime.now() : null,
            valoracio,
            opinio,
            (valoracio != null) ? LocalDateTime.now() : null
        );
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

    public Tasca(
        String id,
        String titol,
        String descripcio,
        String categoria,
        String client,
        String budget,
        String estat
    ) {
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
            "tasca",
            null,
            null
        );
    }
}
