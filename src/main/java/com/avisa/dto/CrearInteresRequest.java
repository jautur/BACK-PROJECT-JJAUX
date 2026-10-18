package com.avisa.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Petició per mostrar interés en una publicació o oferta")
public record CrearInteresRequest(
    @Schema(description = "Identificador de la publicació o oferta", example = "pub-1")
    @NotBlank(message = "L'identificador de la publicació és obligatori")
    String publicacioId,

    @Schema(description = "Identificador del client", example = "u-client")
    String clientId,

    @Schema(description = "Nom del client", example = "Joan Vicent")
    String clientNom,

    @Schema(description = "Missatge opcional del client", example = "Hola, voldria contractar el teu servei.")
    String missatge
) {
}
