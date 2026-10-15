package com.avisa.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Petició per enviar un pressupost a una incidència")
public record CrearPressupostRequest(
    @Schema(description = "Identificador de la incidència", example = "inc-1")
    @NotBlank(message = "L'identificador de la incidència és obligatori")
    String incidenciaId,

    @Schema(description = "Identificador del professional", example = "u-pro")
    String professionalId,

    @Schema(description = "Nom visible del professional", example = "Pintures Xàtiva SL")
    String professionalNom,

    @Schema(description = "Preu proposat", example = "350€")
    @NotBlank(message = "El preu del pressupost és obligatori")
    String preu,

    @Schema(description = "Descripció o abast del pressupost", example = "Pintura plàstica blanca i massillat de grietes.")
    String descripcio,

    @Schema(description = "Duració estimada de la feina", example = "2 dies")
    String duracioEstimada
) {
}
