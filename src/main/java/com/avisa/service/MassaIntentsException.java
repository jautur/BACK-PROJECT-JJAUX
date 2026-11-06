package com.avisa.service;

/**
 * El compte té massa intents fallits de login seguits i està bloquejat temporalment (HTTP 429).
 */
public class MassaIntentsException extends RuntimeException {

	private final long minuts;

	public MassaIntentsException(long minuts) {
		super("Massa intents fallits. Torna-ho a provar d'ací a " + minuts + (minuts == 1 ? " minut." : " minuts."));
		this.minuts = minuts;
	}

	public long minuts() {
		return minuts;
	}
}
