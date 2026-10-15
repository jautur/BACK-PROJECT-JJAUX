package com.avisa.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Petició d'autenticació amb email i contrasenya")
public record LoginRequest(
    @Schema(description = "Correu electrònic de l'usuari", example = "client@avisa.com")
    @NotBlank(message = "L'email és obligatori")
    String email,

    @Schema(description = "Contrasenya d'accés", example = "123456")
    @NotBlank(message = "La contrasenya és obligatòria")
    String password
) {
}
