package com.avisa.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.avisa.service.AvisCreat;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class ServeiPushTest {

	private static final String HOSTS = "fcm.googleapis.com,updates.push.services.mozilla.com,.notify.windows.com,.push.apple.com";

	private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
	private final EnviadorPush enviador = mock(EnviadorPush.class);
	private final ServeiPush servei = new ServeiPush(jdbc, enviador, HOSTS);

	@Test
	@DisplayName("Només s'accepten endpoints https dels serveis de push coneguts")
	void endpointsPermesos() {
		assertThat(servei.endpointPermes("https://fcm.googleapis.com/fcm/send/abc")).isTrue();
		assertThat(servei.endpointPermes("https://updates.push.services.mozilla.com/wpush/v2/abc")).isTrue();
		assertThat(servei.endpointPermes("https://db5p.notify.windows.com/?token=x")).isTrue();
		assertThat(servei.endpointPermes("https://web.push.apple.com/abc")).isTrue();
		assertThat(servei.endpointPermes("https://fcm.googleapis.com:443/fcm/send/abc")).isTrue();
	}

	@Test
	@DisplayName("Rebutja intents d'usar el servidor per cridar altres adreces (SSRF)")
	void endpointsRebutjats() {
		assertThat(servei.endpointPermes(null)).isFalse();
		assertThat(servei.endpointPermes("")).isFalse();
		assertThat(servei.endpointPermes("http://fcm.googleapis.com/x")).isFalse();
		assertThat(servei.endpointPermes("https://localhost/x")).isFalse();
		assertThat(servei.endpointPermes("https://127.0.0.1/x")).isFalse();
		assertThat(servei.endpointPermes("https://169.254.169.254/latest/meta-data")).isFalse();
		assertThat(servei.endpointPermes("https://fcm.googleapis.com@evil.example/x")).isFalse();
		assertThat(servei.endpointPermes("https://fcm.googleapis.com.evil.example/x")).isFalse();
		assertThat(servei.endpointPermes("https://evilfcm.googleapis.com/x")).isFalse();
		assertThat(servei.endpointPermes("https://notify.windows.com/x")).isFalse();
		assertThat(servei.endpointPermes("https://fcm.googleapis.com:8443/x")).isFalse();
		assertThat(servei.endpointPermes("https://fcm.googleapis.com/" + "a".repeat(ServeiPush.MAXIM_ENDPOINT))).isFalse();
		assertThat(servei.endpointPermes("javascript:alert(1)")).isFalse();
	}

	@Test
	@DisplayName("No desa res si l'endpoint o les claus no són vàlids")
	void subscriureInvalid() {
		assertThat(servei.subscriure("u-1", "https://evil.example/x", "k", "a")).isFalse();
		assertThat(servei.subscriure("u-1", "https://fcm.googleapis.com/x", "", "a")).isFalse();
		assertThat(servei.subscriure("u-1", "https://fcm.googleapis.com/x", "k", null)).isFalse();
		verify(jdbc, never()).update(anyString(), ArgumentMatchers.<Object[]>any());
	}

	@Test
	@DisplayName("Una subscripció vàlida es desa")
	void subscriureValid() {
		assertThat(servei.subscriure("u-1", "https://fcm.googleapis.com/x", "clau", "auth")).isTrue();
		verify(jdbc).update(org.mockito.ArgumentMatchers.contains("INSERT INTO subscripcions_push"), eq("u-1"), eq("https://fcm.googleapis.com/x"), eq("clau"), eq("auth"));
	}

	@Test
	@DisplayName("El contingut és JSON vàlid per a ngsw, escapa el text i només enllaça a rutes pròpies")
	void contingutValid() throws Exception {
		String json = ServeiPush.contingut(new AvisCreat("u-1", "Nou \"pressupost\"\n\\ de <b>Joan</b> — 50€", "/tasques-ofertes"));
		JsonNode n = new ObjectMapper().readTree(json).get("notification");

		assertThat(n.get("title").asText()).isEqualTo("AVISA");
		assertThat(n.get("body").asText()).isEqualTo("Nou \"pressupost\"\n\\ de <b>Joan</b> — 50€");
		assertThat(n.at("/data/onActionClick/default/operation").asText()).isEqualTo("navigateLastFocusedOrOpen");
		assertThat(n.at("/data/onActionClick/default/url").asText()).isEqualTo("/tasques-ofertes");
	}

	@Test
	@DisplayName("Un enllaç extern o sense ruta es canvia per l'inici, i un text llarg es retalla")
	void contingutSaneja() throws Exception {
		ObjectMapper om = new ObjectMapper();
		assertThat(om.readTree(ServeiPush.contingut(new AvisCreat("u", "x", "https://evil.example"))).at("/notification/data/onActionClick/default/url").asText()).isEqualTo("/");
		assertThat(om.readTree(ServeiPush.contingut(new AvisCreat("u", "x", "//evil.example"))).at("/notification/data/onActionClick/default/url").asText()).isEqualTo("/");
		assertThat(om.readTree(ServeiPush.contingut(new AvisCreat("u", "x", null))).at("/notification/data/onActionClick/default/url").asText()).isEqualTo("/");
		assertThat(om.readTree(ServeiPush.contingut(new AvisCreat("u", "a".repeat(5000), "/"))).at("/notification/body").asText()).hasSize(ServeiPush.MAXIM_TEXT);
	}

	@SuppressWarnings("unchecked")
	private void subscripcions(SubscripcioPush... s) {
		when(jdbc.query(anyString(), any(RowMapper.class), ArgumentMatchers.<Object[]>any())).thenReturn(List.of(s));
	}

	@Test
	@DisplayName("Un 410 esborra la subscripció, i un error d'enviament no talla els altres dispositius")
	void enviarGestionaErrors() throws Exception {
		SubscripcioPush morta = new SubscripcioPush("id-1", "u-1", "https://fcm.googleapis.com/a", "k", "a");
		SubscripcioPush trencada = new SubscripcioPush("id-2", "u-1", "https://fcm.googleapis.com/b", "k", "a");
		SubscripcioPush bona = new SubscripcioPush("id-3", "u-1", "https://fcm.googleapis.com/c", "k", "a");
		subscripcions(morta, trencada, bona);
		when(enviador.enviar(eq(morta), anyString())).thenReturn(410);
		when(enviador.enviar(eq(trencada), anyString())).thenThrow(new java.io.IOException("xarxa"));
		when(enviador.enviar(eq(bona), anyString())).thenReturn(201);

		servei.enviarAvis(new AvisCreat("u-1", "Hola", "/"));

		verify(enviador).enviar(eq(bona), anyString());
		verify(jdbc).update(org.mockito.ArgumentMatchers.contains("DELETE FROM subscripcions_push WHERE id"), eq("id-1"));
		verify(jdbc, never()).update(org.mockito.ArgumentMatchers.contains("DELETE FROM subscripcions_push WHERE id"), eq("id-3"));
	}

	@Test
	@DisplayName("Si la base falla, enviar un avís no llança cap excepció")
	void enviarNoLlancaMai() {
		when(jdbc.query(anyString(), any(RowMapper.class), ArgumentMatchers.<Object[]>any())).thenThrow(new RuntimeException("base caiguda"));
		servei.enviarAvis(new AvisCreat("u-1", "Hola", "/"));
	}

	@Test
	@DisplayName("Amb el push desactivat no es fa res en crear un avís")
	void desactivat() {
		when(enviador.activat()).thenReturn(false);
		servei.alCrearAvis(new AvisCreat("u-1", "Hola", "/"));
		verify(jdbc, never()).query(anyString(), any(RowMapper.class), ArgumentMatchers.<Object[]>any());
	}
}
