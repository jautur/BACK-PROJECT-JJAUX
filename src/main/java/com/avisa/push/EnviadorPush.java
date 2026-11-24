package com.avisa.push;

/** Envia un missatge Web Push. Separat del servei perquè es puga substituir als tests. */
public interface EnviadorPush {

	/** @return el codi HTTP que ha respost el servei de push del navegador */
	int enviar(SubscripcioPush subscripcio, String contingut) throws Exception;

	/** Si hi ha claus VAPID configurades; sense elles el push queda desactivat i res més canvia. */
	boolean activat();

	/** Clau pública VAPID (base64url) que el navegador necessita per subscriure's, o null. */
	String clauPublica();
}
