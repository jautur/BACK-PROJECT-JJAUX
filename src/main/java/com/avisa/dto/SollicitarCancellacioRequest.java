package com.avisa.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Petició per sol·licitar la cancel·lació d'una tasca en curs")
public record SollicitarCancellacioRequest(
    @Schema(description = "Motiu de la cancel·lació", example = "El professional no s'ha presentat.")
    String motiu
) {
}
