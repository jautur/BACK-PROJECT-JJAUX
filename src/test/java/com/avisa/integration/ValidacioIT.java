package com.avisa.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Les entrades es validen abans d'arribar a la base: 400 amb el motiu, i res es guarda.
 */
class ValidacioIT extends MercatIntegrationTest {

	private String registre(String nom, String rol, String telefon) {
		String tel = telefon == null ? "null" : "\"" + telefon + "\"";
		return "{\"nom\":\"" + nom + "\",\"email\":\"v" + UUID.randomUUID() + "@test.es\",\"password\":\"123456\",\"rol\":\"" + rol
			+ "\",\"telefon\":" + tel + ",\"acceptedTerms\":true,\"acceptedPrivacy\":true}";
	}

	private org.springframework.test.web.servlet.ResultActions registrar(String cos) throws Exception {
		return mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(cos));
	}

	@Test
	@DisplayName("El registre rebutja un nom amb HTML i un professional sense telèfon")
	void registre() throws Exception {
		int abans = files("usuaris");

		registrar(registre("<script>alert(1)</script>", "client", null)).andExpect(status().isBadRequest());
		registrar(registre("Pro sense telèfon", "professional", null)).andExpect(status().isBadRequest());
		registrar(registre("Pro sense telèfon", "professional", "  ")).andExpect(status().isBadRequest());
		registrar(registre("Telèfon estrany", "client", "abc")).andExpect(status().isBadRequest());
		registrar(registre("x".repeat(151), "client", null)).andExpect(status().isBadRequest());
		assertThat(files("usuaris")).isEqualTo(abans);

		registrar(registre("Pro amb telèfon", "professional", "600 123 456")).andExpect(status().isOk());
		registrar(registre("Client sense telèfon", "client", null)).andExpect(status().isOk());
	}

	@Test
	@DisplayName("El perfil rebutja un nom amb HTML")
	void perfil() throws Exception {
		mvc.perform(putCom(client, "/api/users/" + client, "{\"nom\":\"<b>Hola</b>\"}")).andExpect(status().isBadRequest());
		mvc.perform(putCom(client, "/api/users/" + client, "{\"nom\":\"Nom nou\",\"telefon\":\"600111222\"}")).andExpect(status().isOk());
	}

	@Test
	@DisplayName("Publicacions, pressupostos, cancel·lacions i valoracions sense els camps obligatoris donen 400")
	void campsObligatoris() throws Exception {
		mvc.perform(postCom(client, "/api/incidencies", "{\"titol\":\"  \"}")).andExpect(status().isBadRequest());
		mvc.perform(postCom(client, "/api/incidencies", "{\"titol\":\"<img src=x>\"}")).andExpect(status().isBadRequest());
		mvc.perform(postCom(professional, "/api/offers", "{\"categoria\":\"Pintura\",\"note\":\"Sense preu\"}")).andExpect(status().isBadRequest());
		mvc.perform(postCom(professional, "/api/offers", "{\"categoria\":\"Pintura\",\"price\":\"10€\"}")).andExpect(status().isBadRequest());
		assertThat(files("publicacions")).isZero();

		UUID tasca = tasca(publicacio("INCIDENCIA", client, "ASSIGNADA"), client, professional, "EN_CURS");
		mvc.perform(postCom(client, "/api/tasks/" + tasca + "/cancellar", "{\"motiu\":\"\"}")).andExpect(status().isBadRequest());
		mvc.perform(postCom(client, "/api/tasks/" + tasca + "/finalitzar", "{\"valoracio\":9}")).andExpect(status().isBadRequest());
		assertThat(jdbc.queryForObject("SELECT estat FROM tasques WHERE id = ?", String.class, tasca)).isEqualTo("EN_CURS");

		UUID acabada = tasca(publicacio("INCIDENCIA", client, "TANCADA"), client, professional, "FINALITZADA");
		mvc.perform(postCom(client, "/api/tasks/" + acabada + "/avaluar", "{\"valoracio\":0}")).andExpect(status().isBadRequest());
		mvc.perform(postCom(client, "/api/resenyes", "{\"tascaId\":\"" + acabada + "\",\"puntuacio\":6}")).andExpect(status().isBadRequest());
		assertThat(jdbc.queryForObject("SELECT valoracio FROM tasques WHERE id = ?", Integer.class, acabada)).isNull();
	}

	@Test
	@DisplayName("L'administrador no pot resoldre amb una resolució inventada ni crear un avís buit")
	void admin() throws Exception {
		UUID tasca = tasca(publicacio("INCIDENCIA", client, "ASSIGNADA"), client, professional, "EN_REVISIO");
		UUID cancellacio = UUID.randomUUID();
		jdbc.update("INSERT INTO sollicituds_cancellacio (id, tasca_id, sollicitant_id, motiu) VALUES (?, ?, ?, 'Motiu')", cancellacio, tasca, client);

		mvc.perform(postCom(admin, "/api/cancellacions/" + cancellacio + "/resoldre", "{\"resolucio\":\"PERDONAR\"}")).andExpect(status().isBadRequest());
		mvc.perform(postCom(admin, "/api/avisos", "{\"destinatariId\":\"" + client + "\",\"missatge\":\"\"}")).andExpect(status().isBadRequest());
		assertThat(avisosDe(client)).isZero();
	}
}
