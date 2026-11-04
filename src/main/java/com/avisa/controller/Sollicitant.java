package com.avisa.controller;

import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Identitat de qui fa la petició, obtinguda del token JWT ja verificat per Spring Security.
 */
final class Sollicitant {

	private Sollicitant() {}

	static String id(Principal principal) {
		if (principal == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cal iniciar sessió per a fer aquesta acció.");
		}
		return principal.getName();
	}
}
