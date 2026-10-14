package com.avisa.dto;

/**
 * Petició per a finalitzar una tasca i enregistrar opcionalment la valoració i opinió del client.
 */
public record FinalitzarTascaRequest(
    Integer valoracio,
    String opinio
) {}
