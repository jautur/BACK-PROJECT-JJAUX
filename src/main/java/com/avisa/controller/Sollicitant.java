package com.avisa.controller;

import java.security.Principal;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

/**
 * Identitat i rol de qui fa la petició, obtinguts del token JWT ja verificat per Spring Security.
 * Sense token el Principal és null: el client no pot declarar qui és de cap altra manera.
 */
final class Sollicitant {

	private Sollicitant() {}

	static String id(Principal principal) {
		if (principal == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cal iniciar sessió per a fer aquesta acció.");
		}
		return principal.getName();
	}

	/** El rol que porta el token, o buit si no hi ha sessió. */
	static Optional<String> rol(Principal principal) {
		if (principal instanceof JwtAuthenticationToken token) {
			return Optional.ofNullable(token.getToken().getClaimAsString("rol"));
		}
		return Optional.empty();
	}

	static boolean esAdmin(Principal principal) {
		return rol(principal).filter("ADMIN"::equalsIgnoreCase).isPresent();
	}

	/** Sense sessió dona 401; amb sessió d'un altre rol, 403. */
	static void exigirAdmin(Principal principal) {
		id(principal);
		if (!esAdmin(principal)) {
			throw new SecurityException("Només un administrador pot fer aquesta acció.");
		}
	}

	/** Sense sessió dona 401; si la sessió no és de cap dels usuaris indicats ni d'un administrador, 403. */
	static void exigirUnDelsOAdmin(Principal principal, String... usuaris) {
		String id = id(principal);
		if (esAdmin(principal)) {
			return;
		}
		for (String usuari : usuaris) {
			if (id.equalsIgnoreCase(usuari)) {
				return;
			}
		}
		throw new SecurityException("No tens permís per a accedir a les dades d'un altre usuari.");
	}
}
