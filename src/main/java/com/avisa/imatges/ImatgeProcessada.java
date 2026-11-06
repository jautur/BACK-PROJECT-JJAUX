package com.avisa.imatges;

/**
 * Resultat de ProcessadorImatges: les dues mides ja codificades, sense cap metadada de l'original.
 *
 * @param extensio "jpg" o "png" (png només si la imatge té transparència)
 * @param gran     com a màxim 512 px pel costat llarg (pantalles de detall)
 * @param petita   com a màxim 128 px pel costat llarg (llistats)
 */
public record ImatgeProcessada(String extensio, byte[] gran, byte[] petita) {
}
