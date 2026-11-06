package com.avisa.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Petició per sol·licitar la cancel·lació d'una tasca en curs")
public record SollicitarCancellacioRequest(
    @Schema(description = "Motiu de la cancel·lació", example = "El professional no s'ha presentat.")
    @jakarta.validation.constraints.NotBlank(message = "Cal indicar el motiu de la cancel·lació")
    @jakarta.validation.constraints.Size(max = 2000, message = "El motiu pot tenir com a màxim 2000 caràcters")
    String motiu
) {
}
