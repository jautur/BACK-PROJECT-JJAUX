package com.avisa.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;

class ValidadorSessioTest {

	private static final Instant CANVI = Instant.parse("2026-11-24T10:00:00.700Z");

	private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
	private final ValidadorSessio validador = new ValidadorSessio(jdbc);

	private Jwt token(String subjecte, Instant emes) {
		Jwt.Builder b = Jwt.withTokenValue("t").header("alg", "HS256");
		if (subjecte != null) {
			b.subject(subjecte);
		}
		if (emes != null) {
			b.issuedAt(emes).expiresAt(emes.plusSeconds(3600));
		} else {
			b.claim("x", "y");
		}
		return b.build();
	}

	private void canviEn(OffsetDateTime moment) {
		when(jdbc.queryForList(anyString(), eq(OffsetDateTime.class), any(Object[].class)))
			.thenReturn(Collections.singletonList(moment));
	}

	@Test
	@DisplayName("Sense cap canvi de contrasenya, el token val")
	void senseCanvi() {
		canviEn(null);
		assertThat(validador.validate(token("u-1", CANVI)).hasErrors()).isFalse();
	}

	@Test
	@DisplayName("Un token emès abans del canvi de contrasenya es rebutja")
	void tokenAnterior() {
		canviEn(CANVI.atOffset(ZoneOffset.UTC));
		assertThat(validador.validate(token("u-1", CANVI.minusSeconds(60))).hasErrors()).isTrue();
	}

	@Test
	@DisplayName("Un token emès després del canvi val, també el del mateix segon (el login just després de canviar)")
	void tokenPosterior() {
		canviEn(CANVI.atOffset(ZoneOffset.UTC));
		assertThat(validador.validate(token("u-1", Instant.parse("2026-11-24T10:00:00Z"))).hasErrors()).isFalse();
		assertThat(validador.validate(token("u-1", CANVI.plusSeconds(5))).hasErrors()).isFalse();
	}

	@Test
	@DisplayName("Si l'usuari no existeix, no decideix aquest validador")
	void usuariInexistent() {
		when(jdbc.queryForList(anyString(), eq(OffsetDateTime.class), any(Object[].class))).thenReturn(List.of());
		assertThat(validador.validate(token("u-9", CANVI)).hasErrors()).isFalse();
	}

	@Test
	@DisplayName("Un token sense subjecte o sense hora d'emissió es rebutja")
	void tokenIncomplet() {
		assertThat(validador.validate(token(null, CANVI)).hasErrors()).isTrue();
		assertThat(validador.validate(token("u-1", null)).hasErrors()).isTrue();
	}
}
