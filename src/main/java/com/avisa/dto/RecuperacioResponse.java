package com.avisa.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resposta d'una petició de recuperació de contrasenya.
 * No porta mai el codi ni el correu: només diu si la petició s'ha processat i un missatge.
 */
@Schema(description = "Resposta del procés de recuperació de contrasenya")
public record RecuperacioResponse(
    @Schema(description = "Indica si la petició s'ha processat correctament", example = "true")
    boolean success,

    @Schema(description = "Missatge informatiu per a l'usuari", example = "Si el compte existeix, s'ha generat un codi de recuperació.")
    String message
) {
}
