package com.avisa.dto;

public record CrearPressupostRequest(
    String incidenciaId,
    String professionalId,
    String professionalNom,
    String preu,
    String descripcio,
    String duracioEstimada
) {
}

