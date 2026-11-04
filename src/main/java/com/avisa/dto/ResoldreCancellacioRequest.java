package com.avisa.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resolució d'una sol·licitud de cancel·lació per part d'un administrador")
public record ResoldreCancellacioRequest(
    @Schema(description = "SENSE_CONSEQUENCIES, NEGLIGENCIA o REBUTJADA", example = "NEGLIGENCIA")
    String resolucio,

    @Schema(description = "Client o professional responsable, només en cas de negligència")
    String responsableId
) {
}
