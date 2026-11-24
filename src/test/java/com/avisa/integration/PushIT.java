package com.avisa.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.avisa.push.ServeiPush;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Subscripcions push sobre PostgreSQL: unicitat de l'endpoint, tope per usuari i esborrat en cascada. */
class PushIT extends MercatIntegrationTest {

	@Autowired
	private ServeiPush servei;

	private static String endpoint(int n) {
		return "https://fcm.googleapis.com/fcm/send/dispositiu-" + n;
	}

	private int subscripcionsDe(java.util.UUID usuari) {
		return jdbc.queryForObject("SELECT COUNT(*) FROM subscripcions_push WHERE usuari_id = ?", Integer.class, usuari);
	}

	@Test
	@DisplayName("Subscriure dues vegades el mateix endpoint no el duplica, i si canvia de compte passa al nou usuari")
	void endpointUnicIReassignacio() {
		assertThat(servei.subscriure(client.toString(), endpoint(1), "k1", "a1")).isTrue();
		assertThat(servei.subscriure(client.toString(), endpoint(1), "k2", "a2")).isTrue();
		assertThat(files("subscripcions_push")).isEqualTo(1);

		assertThat(servei.subscriure(professional.toString(), endpoint(1), "k3", "a3")).isTrue();

		assertThat(subscripcionsDe(client)).isZero();
		assertThat(subscripcionsDe(professional)).isEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT p256dh FROM subscripcions_push", String.class)).isEqualTo("k3");
	}

	@Test
	@DisplayName("Un usuari conserva com a màxim 10 dispositius")
	void topePerUsuari() {
		for (int i = 0; i < 13; i++) {
			assertThat(servei.subscriure(client.toString(), endpoint(i), "k", "a")).isTrue();
		}
		assertThat(subscripcionsDe(client)).isEqualTo(10);
	}

	@Test
	@DisplayName("Donar de baixa només esborra la subscripció de qui la demana")
	void baixaNomesPropia() {
		servei.subscriure(client.toString(), endpoint(1), "k", "a");

		assertThat(servei.donarDeBaixa(professional.toString(), endpoint(1))).isFalse();
		assertThat(files("subscripcions_push")).isEqualTo(1);
		assertThat(servei.donarDeBaixa(client.toString(), endpoint(1))).isTrue();
		assertThat(files("subscripcions_push")).isZero();
	}

	@Test
	@DisplayName("La base rebutja endpoints que no són https")
	void baseRebutjaHttp() {
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update(
			"INSERT INTO subscripcions_push (usuari_id, endpoint, p256dh, auth) VALUES (?, 'http://fcm.googleapis.com/x', 'k', 'a')", client))
			.hasMessageContaining("subscripcions_push_endpoint_https");
	}

	@Test
	@DisplayName("En esborrar l'usuari s'esborren les seues subscripcions")
	void cascada() {
		servei.subscriure(client.toString(), endpoint(1), "k", "a");
		jdbc.update("DELETE FROM usuaris WHERE id = ?", client);
		assertThat(files("subscripcions_push")).isZero();
	}
}
