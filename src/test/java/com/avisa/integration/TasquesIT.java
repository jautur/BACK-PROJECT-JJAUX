package com.avisa.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

class TasquesIT extends MercatIntegrationTest {

	@Test
	@DisplayName("M'interessa guarda una sol·licitud pendent i avisa el professional, sense crear encara treball acordat")
	void sollicitudGuardada() throws Exception {
		String oferta = oferta();

		String tasca = cos(contractar(client, oferta).andExpect(status().isOk()).andExpect(jsonPath("$.estat").value("Sol·licitada"))).get("id").asText();

		assertThat(estatTasca(tasca)).isEqualTo("SOLLICITADA");
		assertThat(jdbc.queryForObject("SELECT client_id::text FROM tasques WHERE id = ?::uuid AND publicacio_id = ?::uuid AND preu_acordat = '30€'", String.class, tasca, oferta))
			.isEqualTo(client.toString());
		assertThat(avisosDe(professional)).isEqualTo(1);
		mvc.perform(get("/api/tasks")).andExpect(jsonPath("$.length()").value(0));
		mvc.perform(get("/api/interessos/professional/" + professional))
			.andExpect(jsonPath("$[0].id").value(tasca))
			.andExpect(jsonPath("$[0].tascaId").doesNotExist());
		assertThat(estatPublicacio(oferta)).isEqualTo("ACTIVA");
	}

	@Test
	@DisplayName("La sol·licitud és del client del token, no pot duplicar-se i demana sessió i rol de client")
	void validacionsDeSollicitud() throws Exception {
		String oferta = oferta();

		mvc.perform(post("/api/offers/" + oferta + "/contractar")).andExpect(status().isUnauthorized());
		contractar(professional, oferta).andExpect(status().isForbidden());
		contractar(admin, oferta).andExpect(status().isForbidden());
		contractar(client, oferta).andExpect(status().isOk());
		contractar(client, oferta).andExpect(status().isConflict());
		assertThat(files("tasques")).isEqualTo(1);

		mvc.perform(deleteCom(professional, "/api/offers/" + oferta)).andExpect(status().isNoContent());
		contractar(usuari("CLIENT"), oferta).andExpect(status().isConflict());
		contractar(client, UUID.randomUUID().toString()).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Només el professional accepta, i la tasca passa a en curs amb avís al client")
	void acceptarSollicitud() throws Exception {
		String tasca = sollicitud();

		mvc.perform(postCom(usuari("PROFESSIONAL"), "/api/interessos/" + tasca + "/acceptar", "{}")).andExpect(status().isForbidden());
		mvc.perform(postCom(client, "/api/interessos/" + tasca + "/acceptar", "{}")).andExpect(status().isForbidden());
		mvc.perform(postCom(professional, "/api/interessos/" + tasca + "/acceptar", "{}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.estat").value("En curs"));

		assertThat(estatTasca(tasca)).isEqualTo("EN_CURS");
		assertThat(avisosDe(client)).isEqualTo(1);
		mvc.perform(get("/api/tasks")).andExpect(jsonPath("$[0].id").value(tasca));
		mvc.perform(postCom(professional, "/api/interessos/" + tasca + "/acceptar", "{}")).andExpect(status().isConflict());
	}

	@Test
	@DisplayName("El professional pot rebutjar i el client tornar a sol·licitar")
	void rebutjarSollicitud() throws Exception {
		String oferta = oferta();
		String tasca = cos(contractar(client, oferta)).get("id").asText();

		mvc.perform(postCom(professional, "/api/interessos/" + tasca + "/rebutjar", "{}")).andExpect(status().isNoContent());

		assertThat(estatTasca(tasca)).isEqualTo("REBUTJADA");
		assertThat(avisosDe(client)).isEqualTo(1);
		contractar(client, oferta).andExpect(status().isOk());
		assertThat(files("tasques")).isEqualTo(2);
	}

	@Test
	@DisplayName("El client retira la seua sol·licitud sense l'admin, però no la dels altres")
	void retirarSollicitud() throws Exception {
		String tasca = sollicitud();

		mvc.perform(postCom(usuari("CLIENT"), "/api/interessos/" + tasca + "/retirar", "{}")).andExpect(status().isForbidden());
		mvc.perform(postCom(client, "/api/interessos/" + tasca + "/retirar", "{}")).andExpect(status().isNoContent());

		assertThat(estatTasca(tasca)).isEqualTo("RETIRADA");
		assertThat(avisosDe(professional)).isEqualTo(2);
		mvc.perform(postCom(professional, "/api/interessos/" + tasca + "/acceptar", "{}")).andExpect(status().isConflict());
	}

	@Test
	@DisplayName("Client, professional i admin poden finalitzar; un tercer no")
	void finalitzarPermisos() throws Exception {
		mvc.perform(post("/api/tasks/" + tascaEnCurs() + "/finalitzar")).andExpect(status().isUnauthorized());
		finalitzar(usuari("CLIENT"), tascaEnCurs(), "{}").andExpect(status().isForbidden());
		for (UUID qui : new UUID[] {client, professional, admin}) {
			String tasca = tascaEnCurs();
			finalitzar(qui, tasca, "{}").andExpect(status().isOk()).andExpect(jsonPath("$.estat").value("Finalitzada"));
			assertThat(estatTasca(tasca)).isEqualTo("FINALITZADA");
			assertThat(jdbc.queryForObject("SELECT data_finalitzacio IS NOT NULL FROM tasques WHERE id = ?::uuid", Boolean.class, tasca)).isTrue();
		}
	}

	@Test
	@DisplayName("Finalitzar la tasca d'un pressupost tanca la incidència")
	void finalitzarTancaIncidencia() throws Exception {
		String incidencia = cos(mvc.perform(postCom(client, "/api/incidencies", "{\"titol\":\"Fuita\"}"))).get("id").asText();
		String pressupost = cos(mvc.perform(postCom(professional, "/api/pressupostos", "{\"incidenciaId\":\"" + incidencia + "\",\"preu\":\"100€\"}"))).get("id").asText();
		String tasca = cos(mvc.perform(postCom(client, "/api/pressupostos/" + pressupost + "/acceptar", "{}"))).get("id").asText();

		finalitzar(professional, tasca, "{}").andExpect(status().isOk());

		assertThat(estatPublicacio(incidencia)).isEqualTo("TANCADA");
	}

	@Test
	@DisplayName("Finalitzar la tasca d'una oferta deixa l'oferta activa")
	void finalitzarNoRetiraOferta() throws Exception {
		String oferta = oferta();
		String tasca = cos(contractar(client, oferta)).get("id").asText();
		mvc.perform(postCom(professional, "/api/interessos/" + tasca + "/acceptar", "{}"));

		finalitzar(professional, tasca, "{}").andExpect(status().isOk());

		assertThat(estatPublicacio(oferta)).isEqualTo("ACTIVA");
	}

	@Test
	@DisplayName("Una sol·licitud o una tasca ja finalitzada no es poden finalitzar")
	void finalitzarEstatsIncorrectes() throws Exception {
		finalitzar(professional, sollicitud(), "{}").andExpect(status().isConflict());
		String tasca = tascaEnCurs();
		finalitzar(client, tasca, "{}").andExpect(status().isOk());
		finalitzar(client, tasca, "{}").andExpect(status().isConflict());
	}

	@Test
	@DisplayName("Només el client valora, també si ha finalitzat una altra persona, i només una vegada")
	void valorar() throws Exception {
		String tasca = tascaEnCurs();
		avaluar(client, tasca, 5).andExpect(status().isConflict());
		finalitzar(professional, tasca, "{}").andExpect(status().isOk());

		avaluar(professional, tasca, 5).andExpect(status().isForbidden());
		avaluar(admin, tasca, 5).andExpect(status().isForbidden());
		avaluar(client, tasca, 6).andExpect(status().isBadRequest());
		avaluar(client, tasca, 4).andExpect(status().isOk()).andExpect(jsonPath("$.valoracio").value(4)).andExpect(jsonPath("$.comentari").value("bé"));
		avaluar(client, tasca, 3).andExpect(status().isConflict());

		assertThat(jdbc.queryForObject("SELECT comentari FROM tasques WHERE id = ?::uuid", String.class, tasca)).isEqualTo("bé");
		mvc.perform(get("/api/resenyes/treballador/" + professional)).andExpect(jsonPath("$[0].puntuacio").value(4));
	}

	@Test
	@DisplayName("Un professional no pot colar una valoració en finalitzar, i el client sí que pot")
	void valorarEnFinalitzar() throws Exception {
		String tasca = tascaEnCurs();
		finalitzar(professional, tasca, "{\"valoracio\":5}").andExpect(status().isForbidden());
		assertThat(estatTasca(tasca)).isEqualTo("EN_CURS");

		finalitzar(client, tasca, "{\"valoracio\":5,\"comentari\":\"genial\"}").andExpect(status().isOk()).andExpect(jsonPath("$.valoracio").value(5));
	}

	@Test
	@DisplayName("POST /api/resenyes valora la tasca amb les mateixes regles")
	void ressenya() throws Exception {
		String tasca = tascaEnCurs();
		finalitzar(client, tasca, "{}");
		String cos = "{\"tascaId\":\"" + tasca + "\",\"puntuacio\":5,\"descripcio\":\"ok\"}";

		mvc.perform(postCom(professional, "/api/resenyes", cos)).andExpect(status().isForbidden());
		mvc.perform(postCom(client, "/api/resenyes", cos)).andExpect(status().isOk()).andExpect(jsonPath("$.puntuacio").value(5));
		assertThat(jdbc.queryForObject("SELECT valoracio FROM tasques WHERE id = ?::uuid", Integer.class, tasca)).isEqualTo(5);
	}

	@Test
	@DisplayName("Una tasca acordada no s'esborra, una sol·licitud es retira i una incidència s'esborra")
	void esborrar() throws Exception {
		String enCurs = tascaEnCurs();
		mvc.perform(deleteCom(admin, "/api/tasks/" + enCurs)).andExpect(status().isBadRequest());
		assertThat(estatTasca(enCurs)).isEqualTo("EN_CURS");

		String sollicitud = sollicitud();
		mvc.perform(deleteCom(client, "/api/tasks/" + sollicitud)).andExpect(status().isNoContent());
		assertThat(estatTasca(sollicitud)).isEqualTo("RETIRADA");

		String incidencia = cos(mvc.perform(postCom(client, "/api/incidencies", "{\"titol\":\"x\"}"))).get("id").asText();
		mvc.perform(deleteCom(client, "/api/tasks/" + incidencia)).andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("El panell resumeix les dades reals")
	void dashboard() throws Exception {
		tascaEnCurs();
		String acabada = tascaEnCurs();
		finalitzar(client, acabada, "{}");
		mvc.perform(postCom(client, "/api/incidencies", "{\"titol\":\"x\"}"));

		mvc.perform(get("/api/dashboard"))
			.andExpect(jsonPath("$.openTasks").value(1))
			.andExpect(jsonPath("$.activeOffers").value(2))
			.andExpect(jsonPath("$.completedProjects").value(1));
	}

	private String oferta() throws Exception {
		return cos(mvc.perform(postCom(professional, "/api/offers", "{\"categoria\":\"Pintura\",\"price\":\"30€\",\"note\":\"Pinte\"}")).andExpect(status().isOk())).get("id").asText();
	}

	private ResultActions contractar(UUID usuari, String oferta) throws Exception {
		return mvc.perform(postCom(usuari, "/api/offers/" + oferta + "/contractar", "{}"));
	}

	private String sollicitud() throws Exception {
		return cos(contractar(client, oferta())).get("id").asText();
	}

	private String tascaEnCurs() throws Exception {
		String tasca = sollicitud();
		mvc.perform(postCom(professional, "/api/interessos/" + tasca + "/acceptar", "{}")).andExpect(status().isOk());
		return tasca;
	}

	private ResultActions finalitzar(UUID usuari, String tasca, String cos) throws Exception {
		return mvc.perform(postCom(usuari, "/api/tasks/" + tasca + "/finalitzar", cos));
	}

	private ResultActions avaluar(UUID usuari, String tasca, int valoracio) throws Exception {
		return mvc.perform(postCom(usuari, "/api/tasks/" + tasca + "/avaluar", "{\"valoracio\":" + valoracio + ",\"comentari\":\"bé\"}"));
	}

	private String estatTasca(String id) {
		return jdbc.queryForObject("SELECT estat FROM tasques WHERE id = ?::uuid", String.class, id);
	}
}
