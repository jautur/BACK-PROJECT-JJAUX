package com.avisa.push;

import com.avisa.service.AvisCreat;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Notificacions push: guarda les subscripcions dels navegadors i, quan es crea un avís, l'envia als dispositius
 * de l'usuari. L'enviament és en segon pla i mai propaga errors: un push lent o caigut no pot trencar l'avís.
 *
 * Seguretat: l'endpoint el tria el client i el servidor hi fa una petició, així que només s'accepten
 * adreces https dels serveis de push coneguts (avisa.push.hosts-permesos), per evitar que servisca per
 * fer peticions a xarxes internes (SSRF).
 */
@Service
public class ServeiPush {

	private static final Logger log = LoggerFactory.getLogger(ServeiPush.class);

	static final int MAXIM_PER_USUARI = 10;
	static final int MAXIM_ENDPOINT = 2048;
	static final int MAXIM_TEXT = 180;

	private final JdbcTemplate jdbc;
	private final EnviadorPush enviador;
	private final List<String> hostsPermesos;
	private final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
		Thread t = new Thread(r, "push");
		t.setDaemon(true);
		return t;
	});

	public ServeiPush(JdbcTemplate jdbc, EnviadorPush enviador,
			@Value("${avisa.push.hosts-permesos:fcm.googleapis.com,updates.push.services.mozilla.com,.notify.windows.com,.push.apple.com}") String hosts) {
		this.jdbc = jdbc;
		this.enviador = enviador;
		this.hostsPermesos = List.of(hosts.split(",")).stream().map(h -> h.trim().toLowerCase(Locale.ROOT)).filter(h -> !h.isEmpty()).toList();
	}

	public boolean activat() {
		return enviador.activat();
	}

	public String clauPublica() {
		return enviador.clauPublica();
	}

	/** Un endpoint és vàlid si és https, sense credencials ni port estrany, i d'un servei de push conegut. */
	boolean endpointPermes(String endpoint) {
		if (endpoint == null || endpoint.length() > MAXIM_ENDPOINT) {
			return false;
		}
		try {
			URI uri = new URI(endpoint);
			String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
			if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)) {
				return false;
			}
			return hostsPermesos.stream().anyMatch(permes -> permes.startsWith(".") ? host.endsWith(permes) : host.equals(permes));
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Desa (o actualitza) la subscripció d'un navegador. Si l'endpoint ja existia, passa a aquest usuari.
	 * @return false si l'endpoint o les claus no són vàlids
	 */
	public boolean subscriure(String usuariId, String endpoint, String p256dh, String auth) {
		if (!endpointPermes(endpoint) || p256dh == null || p256dh.isBlank() || p256dh.length() > 200 || auth == null || auth.isBlank() || auth.length() > 100) {
			return false;
		}
		jdbc.update("""
			INSERT INTO subscripcions_push (usuari_id, endpoint, p256dh, auth) VALUES (?::uuid, ?, ?, ?)
			ON CONFLICT (endpoint) DO UPDATE SET usuari_id = EXCLUDED.usuari_id, p256dh = EXCLUDED.p256dh, auth = EXCLUDED.auth
			""", usuariId, endpoint, p256dh, auth);
		// Només es conserven les MAXIM_PER_USUARI més noves
		jdbc.update("""
			DELETE FROM subscripcions_push WHERE usuari_id = ?::uuid AND id NOT IN (
				SELECT id FROM subscripcions_push WHERE usuari_id = ?::uuid ORDER BY created_at DESC, id LIMIT ?)
			""", usuariId, usuariId, MAXIM_PER_USUARI);
		return true;
	}

	/** Esborra la subscripció d'aquest usuari amb aquest endpoint (no toca les d'altres usuaris). */
	public boolean donarDeBaixa(String usuariId, String endpoint) {
		return jdbc.update("DELETE FROM subscripcions_push WHERE usuari_id = ?::uuid AND endpoint = ?", usuariId, endpoint) > 0;
	}

	List<SubscripcioPush> subscripcionsDe(String usuariId) {
		return jdbc.query("SELECT id::text, usuari_id::text, endpoint, p256dh, auth FROM subscripcions_push WHERE usuari_id = ?::uuid",
			(rs, n) -> new SubscripcioPush(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)), usuariId);
	}

	@EventListener
	public void alCrearAvis(AvisCreat avis) {
		if (!enviador.activat()) {
			return;
		}
		executor.submit(() -> enviarAvis(avis));
	}

	/** Públic perquè els tests el cridin sense fils; no llança mai. */
	public void enviarAvis(AvisCreat avis) {
		try {
			String contingut = contingut(avis);
			for (SubscripcioPush s : new ArrayList<>(subscripcionsDe(avis.usuariId()))) {
				try {
					int codi = enviador.enviar(s, contingut);
					if (codi == 404 || codi == 410) {
						// El navegador ja no existeix o ha revocat el permís
						jdbc.update("DELETE FROM subscripcions_push WHERE id = ?::uuid", s.id());
					} else if (codi >= 300) {
						log.warn("Push rebutjat (HTTP {}) per a l'usuari {}", codi, avis.usuariId());
					}
				} catch (Exception e) {
					log.warn("No s'ha pogut enviar el push a l'usuari {}: {}", avis.usuariId(), e.toString());
				}
			}
		} catch (Exception e) {
			log.warn("Error preparant el push: {}", e.toString());
		}
	}

	/** Format que entén el service worker d'Angular (ngsw): { notification: { title, body, data.onActionClick } }. */
	static String contingut(AvisCreat avis) {
		String text = avis.missatge() == null ? "" : avis.missatge();
		if (text.length() > MAXIM_TEXT) {
			text = text.substring(0, MAXIM_TEXT - 1) + "…";
		}
		String enllac = avis.enllac() != null && avis.enllac().startsWith("/") && !avis.enllac().startsWith("//") ? avis.enllac() : "/";
		return "{\"notification\":{\"title\":\"AVISA\",\"body\":" + json(text) + ",\"icon\":\"/icons/icon-192x192.png\","
			+ "\"data\":{\"onActionClick\":{\"default\":{\"operation\":\"navigateLastFocusedOrOpen\",\"url\":" + json(enllac) + "}}}}}";
	}

	private static String json(String s) {
		StringBuilder b = new StringBuilder("\"");
		for (char c : s.toCharArray()) {
			switch (c) {
				case '"' -> b.append("\\\"");
				case '\\' -> b.append("\\\\");
				case '\n' -> b.append("\\n");
				case '\r' -> b.append("\\r");
				case '\t' -> b.append("\\t");
				default -> b.append(c < 0x20 ? String.format("\\u%04x", (int) c) : String.valueOf(c));
			}
		}
		return b.append('"').toString();
	}

}
