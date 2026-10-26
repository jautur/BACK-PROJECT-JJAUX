package com.avisa.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * Model que representa un número de telèfon bloquejat a la llista negra d'AVISA.
 * Impedeix el registre de nous comptes, l'actualització de perfils i la verificació
 * amb aquest número per protegir la comunitat contra spam, fraus o morositat.
 */
@Schema(description = "Registre d'un telèfon a la llista negra")
public record BlacklistTelefon(
    @Schema(description = "Identificador únic del registre a la blacklist", example = "BL-001")
    String id,

    @Schema(description = "Número de telèfon bloquejat", example = "600123456")
    String telefon,

    @Schema(description = "Motiu o justificació del bloqueig", example = "Spam reiterat / Perfil fraudulent")
    String motiu,

    @Schema(description = "Nom o identificador de l'administrador que ha aplicat el bloqueig", example = "admin")
    String bloquejatPer,

    @Schema(description = "Data i hora en què es va afegir a la llista negra")
    LocalDateTime createdAt
) {
}

