package com.avisa.dto;

public record TascaRequest(
    String titol,
    String descripcio,
    String categoria,
    String client,
    String professional,
    String budget,
    String estat,
    String tipus
) {
    public TascaRequest(String titol, String descripcio, String categoria, String client, String budget, String estat) {
        this(
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
