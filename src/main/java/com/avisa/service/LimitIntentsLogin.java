package com.avisa.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Límit d'intents fallits de login per compte: 5 fallades en 15 minuts bloquegen aquell compte 15 minuts.
 *
 * Es compta per l'identificador que s'escriu (correu o usuari), existisca o no el compte, perquè la resposta
 * no delate quins comptes hi ha. No es compta per IP: darrere el proxy del front totes les peticions arriben
 * des de la mateixa adreça i un límit per IP bloquejaria tothom alhora.
 * L'estat és en memòria: es perd en reiniciar el backend i no es comparteix entre instàncies.
 */
@Component
public class LimitIntentsLogin {

	static final int FALLADES_MAXIMES = 5;
	static final Duration FINESTRA = Duration.ofMinutes(15);
	static final Duration BLOQUEIG = Duration.ofMinutes(15);

	private final Clock rellotge;
	private final Map<String, Estat> estats = new ConcurrentHashMap<>();

	private static final class Estat {
		private int fallades;
		private Instant primeraFallada;
		private Instant bloquejatFins;
	}

	@Autowired
	public LimitIntentsLogin() {
		this(Clock.systemUTC());
	}

	LimitIntentsLogin(Clock rellotge) {
		this.rellotge = rellotge;
	}

	private static String clau(String identificador) {
		return identificador == null ? "" : identificador.trim().toLowerCase(Locale.ROOT);
	}

	/** Minuts que falten perquè es desbloquege el compte, o 0 si no està bloquejat. */
	public synchronized long minutsBloquejat(String identificador) {
		Estat estat = estats.get(clau(identificador));
		Instant ara = rellotge.instant();
		if (estat == null || estat.bloquejatFins == null || !ara.isBefore(estat.bloquejatFins)) {
			return 0;
		}
		return Math.max(1, (Duration.between(ara, estat.bloquejatFins).toSeconds() + 59) / 60);
	}

	public synchronized void fallada(String identificador) {
		Instant ara = rellotge.instant();
		Estat estat = estats.computeIfAbsent(clau(identificador), k -> new Estat());
		if (estat.primeraFallada == null || ara.isAfter(estat.primeraFallada.plus(FINESTRA))
				|| (estat.bloquejatFins != null && !ara.isBefore(estat.bloquejatFins))) {
			estat.fallades = 0;
			estat.primeraFallada = ara;
			estat.bloquejatFins = null;
		}
		if (++estat.fallades >= FALLADES_MAXIMES) {
			estat.bloquejatFins = ara.plus(BLOQUEIG);
		}
	}

	public synchronized void exit(String identificador) {
		estats.remove(clau(identificador));
	}
}
