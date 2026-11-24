package com.avisa.config;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Rebutja els tokens emesos abans que l'usuari canviara la contrasenya (columna sessions_valides_des_de).
 * Així un token robat o oblidat en un altre dispositiu deixa de valer en canviar la clau, encara que dure una setmana.
 *
 * El token porta l'hora d'emissió en segons, així que la comparació és en segons: un token emès durant el mateix
 * segon que el canvi encara val, i el login fet just després d'un canvi no es rebutja per error.
 * Si l'usuari no existeix, no decideix aquest validador (ho resolen els controladors, com sempre).
 */
@Component
public class ValidadorSessio implements OAuth2TokenValidator<Jwt> {

	private static final OAuth2Error ERROR = new OAuth2Error("invalid_token",
		"La sessió ha caducat perquè s'ha canviat la contrasenya. Torna a iniciar sessió.", null);

	private final JdbcTemplate jdbcTemplate;

	public ValidadorSessio(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public OAuth2TokenValidatorResult validate(Jwt token) {
		if (token.getSubject() == null || token.getIssuedAt() == null) {
			return OAuth2TokenValidatorResult.failure(ERROR);
		}
		List<OffsetDateTime> files = jdbcTemplate.queryForList(
			"SELECT sessions_valides_des_de FROM usuaris WHERE id::text = ?", OffsetDateTime.class, token.getSubject());
		if (files.isEmpty() || files.get(0) == null) {
			return OAuth2TokenValidatorResult.success();
		}
		boolean emesAbans = token.getIssuedAt().isBefore(files.get(0).toInstant().truncatedTo(ChronoUnit.SECONDS));
		return emesAbans ? OAuth2TokenValidatorResult.failure(ERROR) : OAuth2TokenValidatorResult.success();
	}
}
