package com.avisa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OfertaRequest(
    String tascaId,
    String incidenciaId,
    String professional,
    @Size(max = 100, message = "La categoria pot tenir com a màxim 100 caràcters")
    String categoria,
    @NotBlank(message = "El preu és obligatori") @Size(max = 100, message = "El preu pot tenir com a màxim 100 caràcters")
    String price,
    @NotBlank(message = "La descripció de l'oferta és obligatòria") @Size(max = 5000, message = "La descripció pot tenir com a màxim 5000 caràcters")
    String note,
    String estat
) {
    public OfertaRequest ambProfessional(String id) {
        return new OfertaRequest(tascaId, incidenciaId, id, categoria, price, note, estat);
    }

    public String resolveTargetId() {
        if (incidenciaId != null && !incidenciaId.isBlank()) return incidenciaId;
        return tascaId;
    }

    public OfertaRequest(String tascaId, String professional, String categoria, String price, String note, String estat) {
        this(tascaId, tascaId, professional, categoria, price, note, estat);
    }

    public OfertaRequest(String tascaId, String professional, String price, String note, String estat) {
        this(tascaId, tascaId, professional, null, price, note, estat);
    }
}
