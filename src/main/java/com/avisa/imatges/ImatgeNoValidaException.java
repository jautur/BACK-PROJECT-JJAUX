package com.avisa.imatges;

/** La imatge pujada no s'accepta: format no admés, fitxer corrupte o massa gran (en bytes o en píxels). */
public class ImatgeNoValidaException extends RuntimeException {

	private final boolean massaGran;

	ImatgeNoValidaException(String missatge, boolean massaGran) {
		super(missatge);
		this.massaGran = massaGran;
	}

	static ImatgeNoValidaException format(String missatge) {
		return new ImatgeNoValidaException(missatge, false);
	}

	static ImatgeNoValidaException massaGran(String missatge) {
		return new ImatgeNoValidaException(missatge, true);
	}

	/** Cert si s'ha rebutjat per mida (HTTP 413); fals si pel contingut (HTTP 400). */
	public boolean massaGran() {
		return massaGran;
	}
}
