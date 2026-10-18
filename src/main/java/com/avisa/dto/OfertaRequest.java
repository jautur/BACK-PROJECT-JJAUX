package com.avisa.dto;

public record OfertaRequest(
    String tascaId,
    String incidenciaId,
    String professional,
    String categoria,
    String price,
    String note,
    String estat
) {
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
