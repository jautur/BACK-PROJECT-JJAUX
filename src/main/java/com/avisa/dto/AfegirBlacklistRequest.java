package com.avisa.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO per afegir un número de telèfon a la llista negra de la plataforma.
 */
@Schema(description = "Petició per bloquejar un número de telèfon a la blacklist")
public record AfegirBlacklistRequest(
    @Schema(description = "Número de telèfon a bloquejar", example = "600123456")
    @NotBlank(message = "El número de telèfon és obligatori")
    String telefon,

    @Schema(description = "Motiu del bloqueig", example = "Spam / Morositat / Comportament fraudulent")
    String motiu
) {
}

