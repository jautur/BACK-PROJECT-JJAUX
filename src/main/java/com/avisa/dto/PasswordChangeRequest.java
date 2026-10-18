package com.avisa.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO per a la petició de canvi de contrasenya d'un usuari.
 */
@Schema(description = "Petició per canviar la contrasenya d'un compte")
public record PasswordChangeRequest(
    @Schema(description = "Identificador de l'usuari", example = "u-1")
    @NotBlank(message = "L'identificador d'usuari és obligatori")
    String userId,

    @Schema(description = "Nova contrasenya (mínim 6 caràcters)", example = "novaContrasenya123")
    @NotBlank(message = "La nova contrasenya és obligatòria")
    @Size(min = 6, message = "La nova contrasenya ha de tenir com a mínim 6 caràcters")
    String newPassword
) {
}
