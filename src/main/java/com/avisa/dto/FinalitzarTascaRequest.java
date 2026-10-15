package com.avisa.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Petició per a finalitzar una tasca i enregistrar opcionalment la valoració i opinió del client.
 */
@Schema(description = "Petició per finalitzar una tasca i afegir valoració")
public record FinalitzarTascaRequest(
    @Schema(description = "Puntuació de la tasca (1 a 5 estrelles)", example = "5")
    @Min(value = 1, message = "La valoració mínima és 1 estrella")
    @Max(value = 5, message = "La valoració màxima és 5 estrelles")
    Integer valoracio,

    @Schema(description = "Opinió o ressenya escrita", example = "Treball impecable i molt puntual.")
    String opinio
) {}
