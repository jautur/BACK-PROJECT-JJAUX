package com.avisa.dto;

public record CrearInteresRequest(
    String publicacioId,
    String clientId,
    String clientNom,
    String missatge
) {
}

