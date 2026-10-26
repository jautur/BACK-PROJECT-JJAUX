package com.avisa.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO per a la sol·licitud de recuperació de contrasenya mitjançant correu electrònic o usuari.
 */
@Schema(description = "Petició per demanar el restabliment de contrasenya")
public record DemanaRecuperacioRequest(
    @Schema(description = "Correu electrònic o nom d'usuari registrat", example = "cli@avisa.cat")
    @NotBlank(message = "Cal indicar el correu o nom d'usuari")
    String email
) {
}

