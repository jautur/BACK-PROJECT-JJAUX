package com.avisa.model;

public record Oferta(
    String id,
    String tascaId,
    String incidenciaId,
    String professional,
    String categoria,
    String price,
    String note,
    String estat
) {
    public Oferta(String id, String tascaId, String professional, String categoria, String price, String note, String estat) {
        this(id, tascaId, tascaId, professional, categoria, price, note, estat);
    }
}
