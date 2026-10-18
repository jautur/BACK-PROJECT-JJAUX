package com.avisa.dto;

public record TascaRequest(
    String titol,
    String descripcio,
    String categoria,
    String clientId,
    String clientNom,
    String client,
    String professionalId,
    String professionalNom,
    String professional,
    String origenId,
    String budget,
    String estat,
    String tipus,
    Integer valoracio,
    String opinio
) {
    public String resolveClient() {
        if (client != null && !client.isBlank()) return client;
        if (clientNom != null && !clientNom.isBlank()) return clientNom;
        return clientId;
    }

    public String resolveProfessional() {
        if (professional != null && !professional.isBlank()) return professional;
        if (professionalNom != null && !professionalNom.isBlank()) return professionalNom;
        return professionalId;
    }

    public TascaRequest(String titol, String descripcio, String categoria, String client, String professional, String budget, String estat, String tipus) {
        this(titol, descripcio, categoria, null, client, client, null, professional, professional, null, budget, estat, tipus, null, null);
    }

    public TascaRequest(String titol, String descripcio, String categoria, String client, String budget, String estat) {
        this(
            titol,
            descripcio,
            categoria,
            null,
            client,
            client,
            null,
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
