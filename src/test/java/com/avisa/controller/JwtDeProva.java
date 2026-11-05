package com.avisa.controller;

import java.security.Principal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * El Principal que rep un controlador quan la petició porta un token vàlid.
 */
final class JwtDeProva {

	private JwtDeProva() {}

	static Principal sessio(String id, String rol) {
		Jwt jwt = Jwt.withTokenValue("prova")
			.header("alg", "none")
			.subject(id)
			.claim("rol", rol)
			.build();
		return new JwtAuthenticationToken(jwt);
	}
}
