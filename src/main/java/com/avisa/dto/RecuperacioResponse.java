package com.avisa.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resposta generada davant una petició de recuperació de contrasenya.
 */
@Schema(description = "Resposta del procés de recuperació de contrasenya")
public record RecuperacioResponse(
    @Schema(description = "Indica si la petició s'ha processat correctament", example = "true")
    boolean success,

    @Schema(description = "Missatge informatiu per a l'usuari", example = "Si el compte existeix, hem enviat el codi de recuperació.")
    String message,

    @Schema(description = "Correu o usuari afectat", example = "cli@avisa.cat")
    String email,

    @Schema(description = "Codi temporal retornat per a facilitar el test local en absència de servidor SMTP", example = "482915")
    String codiDev
) {
}

