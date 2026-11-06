package com.avisa.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

/**
 * CORS: només els orígens configurats (avisa.cors.origens) poden cridar l'API des d'un navegador,
 * i mai amb credencials. Als tests: http://localhost:* i http://192.168.*.*:*.
 */
class CorsIT extends MercatIntegrationTest {

	private org.springframework.test.web.servlet.ResultActions preflight(String origen, String ruta) throws Exception {
		return mvc.perform(options(ruta)
			.header(HttpHeaders.ORIGIN, origen)
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"));
	}

	@Test
	@DisplayName("Un origen aliè no rep permís CORS, ni en el preflight ni en la petició")
	void origenAlie() throws Exception {
		preflight("http://evil.example", "/api/users").andExpect(status().isForbidden())
			.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		mvc.perform(get("/api/offers").header(HttpHeaders.ORIGIN, "https://evil.example")).andExpect(status().isForbidden())
			.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	@DisplayName("Els orígens configurats poden cridar l'API, sense credencials")
	void origensPermesos() throws Exception {
		for (String origen : new String[] {"http://localhost:4300", "http://192.168.1.20:4300"}) {
			preflight(origen, "/api/users").andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origen))
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
			mvc.perform(getCom(client, "/api/tasks").header(HttpHeaders.ORIGIN, origen)).andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origen));
		}
	}

	@Test
	@DisplayName("Una petició sense Origin (el proxy del front, curl) no es veu afectada")
	void senseOrigen() throws Exception {
		mvc.perform(get("/api/offers")).andExpect(status().isOk());
	}
}
