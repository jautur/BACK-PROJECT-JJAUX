package com.avisa.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LimitIntentsLoginTest {

	private final AtomicReference<Instant> ara = new AtomicReference<>(Instant.parse("2026-11-06T10:00:00Z"));
	private final LimitIntentsLogin limit = new LimitIntentsLogin(new Clock() {
		@Override public ZoneId getZone() { return ZoneOffset.UTC; }
		@Override public Clock withZone(ZoneId zone) { return this; }
		@Override public Instant instant() { return ara.get(); }
	});

	private void fallar(String compte, int vegades) {
		for (int i = 0; i < vegades; i++) {
			limit.fallada(compte);
		}
	}

	private void passar(Duration temps) {
		ara.set(ara.get().plus(temps));
	}

	@Test
	@DisplayName("4 fallades no bloquegen; la cinquena bloqueja 15 minuts")
	void cincFalladesBloquegen() {
		fallar("cli", 4);
		assertThat(limit.minutsBloquejat("cli")).isZero();

		limit.fallada("cli");
		assertThat(limit.minutsBloquejat("cli")).isEqualTo(15);

		passar(Duration.ofMinutes(14));
		assertThat(limit.minutsBloquejat("cli")).isEqualTo(1);
		passar(Duration.ofMinutes(1));
		assertThat(limit.minutsBloquejat("cli")).isZero();
	}

	@Test
	@DisplayName("Les fallades caduquen passats 15 minuts des de la primera")
	void finestra() {
		fallar("cli", 4);
		passar(Duration.ofMinutes(16));
		limit.fallada("cli");

		assertThat(limit.minutsBloquejat("cli")).isZero();
	}

	@Test
	@DisplayName("Un login correcte posa el comptador a zero")
	void exitReinicia() {
		fallar("cli", 4);
		limit.exit("cli");
		fallar("cli", 4);

		assertThat(limit.minutsBloquejat("cli")).isZero();
	}

	@Test
	@DisplayName("Cada compte compta per separat, sense distingir majúscules ni espais")
	void perCompte() {
		fallar(" CLI ", 5);

		assertThat(limit.minutsBloquejat("cli")).isPositive();
		assertThat(limit.minutsBloquejat("pro")).isZero();
	}

	@Test
	@DisplayName("Passat el bloqueig, el compte torna a tindre 5 intents")
	void despresDelBloqueig() {
		fallar("cli", 5);
		passar(LimitIntentsLogin.BLOQUEIG);
		fallar("cli", 4);

		assertThat(limit.minutsBloquejat("cli")).isZero();
	}
}
