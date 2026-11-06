package com.avisa.integration;

/**
 * La base només accepta contrasenyes amb hash BCrypt (migració V2).
 */
final class ContrasenyaDeProva {

	/** La contrasenya en clar dels usuaris de prova. */
	static final String CLAR = "secreta";

	/** El seu hash BCrypt. */
	static final String HASH = "$2a$10$n5f0jmVZUnF5mzfnxuBFUOaUGBZ.MDhQug1rbtTUoeCyj/3rTUE8K";

	private ContrasenyaDeProva() {}
}
