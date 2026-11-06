package com.avisa.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class PublicacionsIT extends MercatIntegrationTest {

	@Test
	@DisplayName("Crear una incidència la guarda de veritat amb el client del token")
	void incidenciaGuardada() throws Exception {
		String id = crearIncidencia(client);

		assertThat(jdbc.queryForObject("SELECT autor_id::text FROM publicacions WHERE id = ?::uuid AND tipus = 'INCIDENCIA' AND estat = 'OBERTA' AND titol = 'Fuita'",
			String.class, id)).isEqualTo(client.toString());
		mvc.perform(get("/api/incidencies")).andExpect(jsonPath("$[0].id").value(id)).andExpect(jsonPath("$[0].estat").value("Oberta"));
		mvc.perform(get("/api/tasks/disponibles")).andExpect(jsonPath("$[0].id").value(id));
	}

	@Test
	@DisplayName("Crear una oferta la guarda de veritat i apareix al mercat")
	void ofertaGuardada() throws Exception {
		String id = crearOferta(professional);

		assertThat(jdbc.queryForObject("SELECT autor_id::text FROM publicacions WHERE id = ?::uuid AND tipus = 'OFERTA' AND estat = 'ACTIVA' AND preu = '30€'",
			String.class, id)).isEqualTo(professional.toString());
		mvc.perform(get("/api/offers")).andExpect(jsonPath("$[0].id").value(id)).andExpect(jsonPath("$[0].estat").value("Pendent"))
			.andExpect(jsonPath("$[0].professionalId").value(professional.toString()));
	}

	@Test
	@DisplayName("Un error de la base dona 500 i no fingeix que s'ha guardat")
	void errorDeBaseNoFingeixExit() throws Exception {
		jdbc.execute("ALTER TABLE publicacions RENAME TO publicacions_amagada");
		try {
			mvc.perform(postCom(client, "/api/incidencies", "{\"titol\":\"Avaria\",\"categoria\":\"c\"}"))
				.andExpect(status().isInternalServerError());
		} finally {
			jdbc.execute("ALTER TABLE publicacions_amagada RENAME TO publicacions");
		}

		assertThat(files("publicacions")).isZero();
	}

	@Test
	@DisplayName("Un títol més llarg que la columna es rebutja amb 400 abans d'arribar a la base")
	void titolMassaLlarg() throws Exception {
		mvc.perform(postCom(client, "/api/incidencies", "{\"titol\":\"" + "x".repeat(300) + "\",\"categoria\":\"c\"}"))
			.andExpect(status().isBadRequest());

		assertThat(files("publicacions")).isZero();
	}

	@Test
	@DisplayName("Sense autor existent o amb el rol equivocat no es crea res")
	void autorInvalid() throws Exception {
		mvc.perform(post("/api/incidencies").contentType(MediaType.APPLICATION_JSON).content("{\"titol\":\"t\",\"client\":\"ningú\"}"))
			.andExpect(status().isUnauthorized());
		mvc.perform(postCom(client, "/api/offers", "{\"price\":\"1\",\"note\":\"n\"}")).andExpect(status().isForbidden());
		mvc.perform(postCom(professional, "/api/incidencies", "{\"titol\":\"t\"}")).andExpect(status().isForbidden());

		assertThat(files("publicacions")).isZero();
	}

	@Test
	@DisplayName("Una incidència sense títol es rebutja")
	void incidenciaSenseTitol() throws Exception {
		mvc.perform(postCom(client, "/api/incidencies", "{\"categoria\":\"c\"}")).andExpect(status().isBadRequest());
		assertThat(files("publicacions")).isZero();
	}

	@Test
	@DisplayName("Retirar una oferta rebutja les sol·licituds pendents, avisa els clients i respecta les tasques en curs")
	void retirarOferta() throws Exception {
		String oferta = crearOferta(professional);
		UUID altreClient = usuari("CLIENT");
		UUID sollicitada = tasca(oferta, client, "SOLLICITADA", null);
		UUID enCurs = tasca(oferta, altreClient, "EN_CURS", "now()");

		mvc.perform(deleteCom(professional, "/api/offers/" + oferta)).andExpect(status().isNoContent());

		assertThat(estatPublicacio(oferta)).isEqualTo("RETIRADA");
		assertThat(estatTasca(sollicitada)).isEqualTo("REBUTJADA");
		assertThat(estatTasca(enCurs)).isEqualTo("EN_CURS");
		assertThat(avisosDe(client)).isEqualTo(1);
		assertThat(avisosDe(altreClient)).isZero();
		mvc.perform(get("/api/offers")).andExpect(jsonPath("$.length()").value(0));
		mvc.perform(get("/api/offers/" + oferta)).andExpect(jsonPath("$.estat").value("Retirada"));
	}

	@Test
	@DisplayName("Només l'autor o un administrador poden retirar una oferta")
	void retirarOfertaPermisos() throws Exception {
		String oferta = crearOferta(professional);

		mvc.perform(delete("/api/offers/" + oferta)).andExpect(status().isUnauthorized());
		mvc.perform(deleteCom(usuari("PROFESSIONAL"), "/api/offers/" + oferta)).andExpect(status().isForbidden());
		assertThat(estatPublicacio(oferta)).isEqualTo("ACTIVA");

		mvc.perform(deleteCom(admin, "/api/offers/" + oferta)).andExpect(status().isNoContent());
		assertThat(estatPublicacio(oferta)).isEqualTo("RETIRADA");
		mvc.perform(deleteCom(admin, "/api/offers/" + UUID.randomUUID())).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Una incidència amb historial no s'esborra; sense historial, sí")
	void esborrarIncidencia() throws Exception {
		String ambPressupost = crearIncidencia(client);
		jdbc.update("INSERT INTO pressupostos (publicacio_id, professional_id, preu) VALUES (?::uuid, ?, '1')", ambPressupost, professional);
		String sensePressupost = crearIncidencia(client);

		mvc.perform(deleteCom(client, "/api/incidencies/" + ambPressupost)).andExpect(status().isBadRequest());
		mvc.perform(deleteCom(usuari("CLIENT"), "/api/incidencies/" + sensePressupost)).andExpect(status().isForbidden());
		mvc.perform(deleteCom(client, "/api/incidencies/" + sensePressupost)).andExpect(status().isNoContent());

		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM publicacions", Integer.class)).isEqualTo(1);
	}

	@Test
	@DisplayName("Un identificador que no és UUID dona 404, no un error del servidor")
	void identificadorAntic() throws Exception {
		mvc.perform(get("/api/incidencies/I-101")).andExpect(status().isNotFound());
		mvc.perform(get("/api/offers/O-201")).andExpect(status().isNotFound());
	}

	private String crearIncidencia(UUID autor) throws Exception {
		return cos(mvc.perform(postCom(autor, "/api/incidencies",
				"{\"titol\":\"Fuita\",\"descripcio\":\"d\",\"categoria\":\"Fontaneria\",\"budget\":\"100€\"}"))
			.andExpect(status().isOk())).get("id").asText();
	}

	private String crearOferta(UUID autor) throws Exception {
		return cos(mvc.perform(postCom(autor, "/api/offers", "{\"categoria\":\"Pintura\",\"price\":\"30€\",\"note\":\"Pinte\"}"))
			.andExpect(status().isOk())).get("id").asText();
	}

	private UUID tasca(String publicacio, UUID clientTasca, String estat, String inici) {
		UUID id = UUID.randomUUID();
		jdbc.update("INSERT INTO tasques (id, publicacio_id, client_id, professional_id, preu_acordat, estat, data_inici) VALUES (?, ?::uuid, ?, ?, '30€', ?, " + inici + ")",
			id, publicacio, clientTasca, professional, estat);
		return id;
	}

	private String estatTasca(UUID id) {
		return jdbc.queryForObject("SELECT estat FROM tasques WHERE id = ?", String.class, id);
	}
}
