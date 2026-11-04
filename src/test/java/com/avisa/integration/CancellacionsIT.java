package com.avisa.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

class CancellacionsIT extends MercatIntegrationTest {

	private String incidencia;
	private String pressupostTriat;
	private String pressupostAuto;
	private String pressupostManual;
	private UUID proAuto;
	private UUID proManual;
	private String tasca;

	@BeforeEach
	void tascaDePressupost() throws Exception {
		proAuto = usuari("PROFESSIONAL");
		proManual = usuari("PROFESSIONAL");
		incidencia = cos(mvc.perform(postCom(client, "/api/incidencies", "{\"titol\":\"Fuita\"}"))).get("id").asText();
		pressupostTriat = pressupost(professional);
		pressupostAuto = pressupost(proAuto);
		pressupostManual = pressupost(proManual);
		mvc.perform(postCom(client, "/api/pressupostos/" + pressupostManual + "/rebutjar", "{}")).andExpect(status().isNoContent());
		tasca = cos(mvc.perform(postCom(client, "/api/pressupostos/" + pressupostTriat + "/acceptar", "{}")).andExpect(status().isOk())).get("id").asText();
		jdbc.update("DELETE FROM avisos");
	}

	@Test
	@DisplayName("Sol·licitar deixa la tasca en revisió, sense poder-se finalitzar, i avisa l'altra part i tots els admins")
	void sollicitar() throws Exception {
		UUID altreAdmin = usuari("ADMIN");

		sollicitar(client, tasca).andExpect(status().isOk()).andExpect(jsonPath("$.estat").value("En revisió"));

		assertThat(estatTasca()).isEqualTo("EN_REVISIO");
		assertThat(avisosDe(professional)).isEqualTo(1);
		assertThat(avisosDe(admin)).isEqualTo(1);
		assertThat(avisosDe(altreAdmin)).isEqualTo(1);
		assertThat(avisosDe(client)).isZero();
		assertThat(jdbc.queryForObject("SELECT sollicitant_id::text FROM sollicituds_cancellacio WHERE tasca_id = ?::uuid AND motiu = 'No ve'", String.class, tasca))
			.isEqualTo(client.toString());
		mvc.perform(postCom(client, "/api/tasks/" + tasca + "/finalitzar", "{}")).andExpect(status().isConflict());
		mvc.perform(postCom(admin, "/api/tasks/" + tasca + "/finalitzar", "{}")).andExpect(status().isConflict());
		sollicitar(professional, tasca).andExpect(status().isConflict());
	}

	@Test
	@DisplayName("Només el client o el professional la sol·liciten, sempre amb motiu")
	void sollicitarPermisos() throws Exception {
		sollicitar(admin, tasca).andExpect(status().isForbidden());
		sollicitar(usuari("CLIENT"), tasca).andExpect(status().isForbidden());
		mvc.perform(postCom(client, "/api/tasks/" + tasca + "/cancellar", "{\"motiu\":\"  \"}")).andExpect(status().isBadRequest());

		assertThat(estatTasca()).isEqualTo("EN_CURS");
		assertThat(files("sollicituds_cancellacio")).isZero();
	}

	@Test
	@DisplayName("Cancel·lar reobri la incidència: el triat queda rebutjat, l'automàtic torna a pendent avisat i el manual no")
	void cancellarReobreIncidencia() throws Exception {
		resoldre(admin, sollicitud(client), "SENSE_CONSEQUENCIES", null).andExpect(status().isOk()).andExpect(jsonPath("$.adminId").value(admin.toString()));

		assertThat(estatTasca()).isEqualTo("CANCELLADA");
		assertThat(estatPublicacio(incidencia)).isEqualTo("OBERTA");
		assertThat(estatPressupost(pressupostTriat)).isEqualTo("REBUTJAT");
		assertThat(estatPressupost(pressupostAuto)).isEqualTo("PENDENT");
		assertThat(estatPressupost(pressupostManual)).isEqualTo("REBUTJAT");
		assertThat(avisosDe(proAuto)).isEqualTo(1);
		assertThat(avisosDe(proManual)).isZero();
		assertThat(avisosDe(client)).isEqualTo(1);
	}

	@Test
	@DisplayName("Després de cancel·lar, el client pot triar un altre professional, però no repetir amb el rebutjat")
	void triarUnAltre() throws Exception {
		resoldre(admin, sollicitud(client), "SENSE_CONSEQUENCIES", null).andExpect(status().isOk());

		mvc.perform(postCom(client, "/api/pressupostos/" + pressupostTriat + "/acceptar", "{}")).andExpect(status().isConflict());
		mvc.perform(postCom(client, "/api/pressupostos/" + pressupostAuto + "/acceptar", "{}")).andExpect(status().isOk());

		assertThat(estatPublicacio(incidencia)).isEqualTo("ASSIGNADA");
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tasques WHERE estat = 'EN_CURS' AND professional_id = ?", Integer.class, proAuto)).isEqualTo(1);
	}

	@Test
	@DisplayName("Rebutjar la cancel·lació torna la tasca a en curs, sense tocar la incidència, i es pot tornar a sol·licitar")
	void rebutjada() throws Exception {
		resoldre(admin, sollicitud(client), "REBUTJADA", null).andExpect(status().isOk());

		assertThat(estatTasca()).isEqualTo("EN_CURS");
		assertThat(estatPublicacio(incidencia)).isEqualTo("ASSIGNADA");
		assertThat(estatPressupost(pressupostTriat)).isEqualTo("ACCEPTAT");
		assertThat(estatPressupost(pressupostAuto)).isEqualTo("REBUTJAT");

		sollicitar(professional, tasca).andExpect(status().isOk());
		assertThat(files("sollicituds_cancellacio")).isEqualTo(2);
	}

	@Test
	@DisplayName("El responsable d'una negligència ha de ser el client o el professional d'aquesta tasca")
	void negligenciaValidaResponsable() throws Exception {
		String id = sollicitud(client);
		UUID alie = usuari("PROFESSIONAL");

		resoldre(admin, id, "NEGLIGENCIA", alie.toString()).andExpect(status().isBadRequest());
		resoldre(admin, id, "NEGLIGENCIA", admin.toString()).andExpect(status().isBadRequest());
		resoldre(admin, id, "NEGLIGENCIA", null).andExpect(status().isBadRequest());
		resoldre(admin, id, "SENSE_CONSEQUENCIES", client.toString()).andExpect(status().isBadRequest());
		resoldre(admin, id, "INVENTADA", null).andExpect(status().isBadRequest());
		assertThat(estatTasca()).isEqualTo("EN_REVISIO");

		resoldre(admin, id, "NEGLIGENCIA", professional.toString()).andExpect(status().isOk()).andExpect(jsonPath("$.responsableId").value(professional.toString()));

		assertThat(estatTasca()).isEqualTo("CANCELLADA");
		assertThat(jdbc.queryForObject("SELECT data_resolucio IS NOT NULL AND admin_id = ? FROM sollicituds_cancellacio WHERE id = ?::uuid", Boolean.class, admin, id)).isTrue();
	}

	@Test
	@DisplayName("La negligència queda a l'historial, només visible per a l'admin, i no té conseqüències automàtiques")
	void negligenciaNomesPerAdmin() throws Exception {
		jdbc.update("UPDATE usuaris SET telefon = '600111222' WHERE id = ?", professional);
		resoldre(admin, sollicitud(client), "NEGLIGENCIA", professional.toString()).andExpect(status().isOk());

		String ruta = "/api/cancellacions/negligencies/" + professional;
		mvc.perform(ambToken(admin, ruta)).andExpect(status().isOk()).andExpect(jsonPath("$[0].tascaId").value(tasca)).andExpect(jsonPath("$[0].motiu").value("No ve"));
		mvc.perform(ambToken(professional, ruta)).andExpect(status().isForbidden());
		mvc.perform(ambToken(client, ruta)).andExpect(status().isForbidden());
		mvc.perform(get(ruta)).andExpect(status().isUnauthorized());

		assertThat(files("blacklist_telefons")).isZero();
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM avisos WHERE missatge ILIKE '%neglig%'", Integer.class)).isZero();
	}

	@Test
	@DisplayName("Només un admin resol, una sola vegada, i només un admin consulta el llistat")
	void resoldrePermisos() throws Exception {
		String id = sollicitud(client);

		resoldre(client, id, "SENSE_CONSEQUENCIES", null).andExpect(status().isForbidden());
		resoldre(professional, id, "SENSE_CONSEQUENCIES", null).andExpect(status().isForbidden());
		mvc.perform(ambToken(client, "/api/cancellacions")).andExpect(status().isForbidden());
		mvc.perform(ambToken(admin, "/api/cancellacions").param("pendents", "true")).andExpect(jsonPath("$[0].id").value(id));

		resoldre(admin, id, "SENSE_CONSEQUENCIES", null).andExpect(status().isOk());
		resoldre(admin, id, "REBUTJADA", null).andExpect(status().isConflict());
		resoldre(admin, UUID.randomUUID().toString(), "REBUTJADA", null).andExpect(status().isNotFound());
		mvc.perform(ambToken(admin, "/api/cancellacions").param("pendents", "true")).andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	@DisplayName("Cancel·lar la tasca d'una oferta no toca l'oferta")
	void cancellarTascaOferta() throws Exception {
		String oferta = cos(mvc.perform(postCom(professional, "/api/offers", "{\"price\":\"1\",\"note\":\"n\"}"))).get("id").asText();
		String deOferta = cos(mvc.perform(postCom(client, "/api/offers/" + oferta + "/contractar", "{}"))).get("id").asText();
		mvc.perform(postCom(professional, "/api/interessos/" + deOferta + "/acceptar", "{}")).andExpect(status().isOk());
		sollicitar(professional, deOferta).andExpect(status().isOk());
		String id = jdbc.queryForObject("SELECT id::text FROM sollicituds_cancellacio WHERE tasca_id = ?::uuid", String.class, deOferta);

		resoldre(admin, id, "SENSE_CONSEQUENCIES", null).andExpect(status().isOk());

		assertThat(estatPublicacio(oferta)).isEqualTo("ACTIVA");
		assertThat(jdbc.queryForObject("SELECT estat FROM tasques WHERE id = ?::uuid", String.class, deOferta)).isEqualTo("CANCELLADA");
		assertThat(estatPublicacio(incidencia)).isEqualTo("ASSIGNADA");
	}

	private String pressupost(UUID pro) throws Exception {
		return cos(mvc.perform(postCom(pro, "/api/pressupostos", "{\"incidenciaId\":\"" + incidencia + "\",\"preu\":\"100€\"}")).andExpect(status().isCreated())).get("id").asText();
	}

	private ResultActions sollicitar(UUID usuari, String idTasca) throws Exception {
		return mvc.perform(postCom(usuari, "/api/tasks/" + idTasca + "/cancellar", "{\"motiu\":\"No ve\"}"));
	}

	private String sollicitud(UUID usuari) throws Exception {
		sollicitar(usuari, tasca).andExpect(status().isOk());
		jdbc.update("DELETE FROM avisos");
		return jdbc.queryForObject("SELECT id::text FROM sollicituds_cancellacio WHERE tasca_id = ?::uuid AND resolucio IS NULL", String.class, tasca);
	}

	private ResultActions resoldre(UUID usuari, String id, String resolucio, String responsable) throws Exception {
		String resp = responsable == null ? "null" : "\"" + responsable + "\"";
		return mvc.perform(postCom(usuari, "/api/cancellacions/" + id + "/resoldre", "{\"resolucio\":\"" + resolucio + "\",\"responsableId\":" + resp + "}"));
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder ambToken(UUID usuari, String ruta) {
		String token = deleteCom(usuari, ruta).buildRequest(new org.springframework.mock.web.MockServletContext()).getHeader(HttpHeaders.AUTHORIZATION);
		return get(ruta).header(HttpHeaders.AUTHORIZATION, token);
	}

	private String estatTasca() {
		return jdbc.queryForObject("SELECT estat FROM tasques WHERE id = ?::uuid", String.class, tasca);
	}

	private String estatPressupost(String id) {
		return jdbc.queryForObject("SELECT estat FROM pressupostos WHERE id = ?::uuid", String.class, id);
	}
}
