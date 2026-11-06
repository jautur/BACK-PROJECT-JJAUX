package com.avisa.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Després de 5 intents fallits, el compte queda bloquejat 15 minuts encara que la contrasenya siga bona.
 */
class LimitLoginIT extends MercatIntegrationTest {

	private MockHttpServletRequestBuilder login(String email, String password) {
		return post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
	}

	@Test
	@DisplayName("La sisena petició dona 429 encara que la contrasenya siga bona, i els altres comptes no es veuen afectats")
	void bloqueja() throws Exception {
		UUID usuari = usuari("CLIENT");
		String email = usuari + "@test.es";
		for (int i = 0; i < 5; i++) {
			mvc.perform(login(email, "dolenta")).andExpect(status().isUnauthorized());
		}

		mvc.perform(login(email, ContrasenyaDeProva.CLAR)).andExpect(status().isTooManyRequests())
			.andExpect(header().string("Retry-After", "900"))
			.andExpect(jsonPath("$.error", Matchers.containsString("15 minuts")));
		mvc.perform(login(client + "@test.es", ContrasenyaDeProva.CLAR)).andExpect(status().isOk());
	}

	@Test
	@DisplayName("Un compte inexistent es bloqueja igual, per no delatar quins comptes existeixen")
	void compteInexistent() throws Exception {
		String email = "ningu-" + UUID.randomUUID() + "@test.es";
		for (int i = 0; i < 5; i++) {
			mvc.perform(login(email, "dolenta")).andExpect(status().isUnauthorized());
		}

		mvc.perform(login(email, "dolenta")).andExpect(status().isTooManyRequests());
	}
}
