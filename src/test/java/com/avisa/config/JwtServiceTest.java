package com.avisa.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;

class JwtServiceTest {

	private static final String SECRET = "un-secret-de-prova-prou-llarg-0123456789";

	@Test
	@DisplayName("Sense secret, o amb un de massa curt, l'aplicació no arrenca")
	void exigeixSecret() {
		assertThatThrownBy(() -> new JwtService(null)).isInstanceOf(IllegalStateException.class).hasMessageContaining("AVISA_JWT_SECRET");
		assertThatThrownBy(() -> new JwtService("  ")).isInstanceOf(IllegalStateException.class).hasMessageContaining("AVISA_JWT_SECRET");
		assertThatThrownBy(() -> new JwtService("curt")).isInstanceOf(IllegalStateException.class).hasMessageContaining("massa curt");
	}

	@Test
	@DisplayName("Un token només val amb el mateix secret amb què s'ha signat")
	void tokenSignatAmbUnAltreSecret() {
		JwtService actual = new JwtService(SECRET);
		JwtService altre = new JwtService("un-altre-secret-tambe-prou-llarg-987654321");
		String token = altre.generateToken("u-1", "a@test.es", "ADMIN");

		assertThat(actual.jwtDecoder().decode(actual.generateToken("u-1", "a@test.es", "ADMIN")).getSubject()).isEqualTo("u-1");
		assertThatThrownBy(() -> actual.jwtDecoder().decode(token)).isInstanceOf(JwtException.class);
	}
}
