package com.avisa.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO per a l'aplicació del restabliment de contrasenya amb el codi temporal obtingut.
 */
@Schema(description = "Petició per confirmar la nova contrasenya amb el codi de recuperació")
public record RestablirContrasenyaRequest(
    @Schema(description = "Correu electrònic o nom d'usuari registrat", example = "cli@avisa.cat")
    @NotBlank(message = "El correu o usuari és obligatori")
    String email,

    @Schema(description = "Codi temporal de 6 dígits o token de verificació", example = "482915")
    @NotBlank(message = "El codi de verificació és obligatori")
    String codi,

    @Schema(description = "Nova contrasenya d'accés (mínim 6 caràcters)", example = "novaContrasenya123")
    @NotBlank(message = "La nova contrasenya és obligatòria")
    @Size(min = 6, message = "La nova contrasenya ha de tenir com a mínim 6 caràcters")
    String novaContrasenya
) {
}

