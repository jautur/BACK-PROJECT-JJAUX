package com.avisa.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PressupostosIT extends MercatIntegrationTest {

	private String incidencia;

	@BeforeEach
	void incidenciaOberta() throws Exception {
		incidencia = cos(mvc.perform(postCom(client, "/api/incidencies", "{\"titol\":\"Fuita\",\"categoria\":\"Fontaneria\"}"))
			.andExpect(status().isOk())).get("id").asText();
	}

	@Test
	@DisplayName("Dos pressupostos de professionals diferents queden tots dos guardats")
	void dosPressupostos() throws Exception {
		String primer = pressupost(professional, "100€");
		String segon = pressupost(usuari("PROFESSIONAL"), "120€");

		assertThat(primer).isNotEqualTo(segon);
		assertThat(files("pressupostos")).isEqualTo(2);
		assertThat(avisosDe(client)).isEqualTo(2);
	}

	@Test
	@DisplayName("Vint pressupostos en paral·lel es guarden tots")
	void vintEnParalel() throws Exception {
		List<Callable<Integer>> peticions = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			UUID pro = usuari("PROFESSIONAL");
			peticions.add(() -> mvc.perform(postCom(pro, "/api/pressupostos", cosPressupost("50€")))
				.andReturn().getResponse().getStatus());
		}

		assertThat(enParalel(peticions)).containsOnly(201).hasSize(20);
		assertThat(files("pressupostos")).isEqualTo(20);
		assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT id) FROM pressupostos", Integer.class)).isEqualTo(20);
	}

	@Test
	@DisplayName("Un sol pressupost per professional i incidència")
	void unPerProfessional() throws Exception {
		pressupost(professional, "100€");

		mvc.perform(postCom(professional, "/api/pressupostos", cosPressupost("90€"))).andExpect(status().isConflict());

		assertThat(files("pressupostos")).isEqualTo(1);
	}

	@Test
	@DisplayName("Només un professional pot pressupostar, i només una incidència oberta que existisca")
	void validacionsEnCrear() throws Exception {
		mvc.perform(postCom(client, "/api/pressupostos", cosPressupost("1"))).andExpect(status().isForbidden());
		mvc.perform(postCom(professional, "/api/pressupostos",
			"{\"incidenciaId\":\"" + UUID.randomUUID() + "\",\"preu\":\"1\"}")).andExpect(status().isBadRequest());
		assertThat(files("pressupostos")).isZero();

		String acceptat = pressupost(professional, "100€");
		acceptar(client, acceptat).andExpect(status().isOk());
		mvc.perform(postCom(usuari("PROFESSIONAL"), "/api/pressupostos", cosPressupost("1"))).andExpect(status().isConflict());
	}

	@Test
	@DisplayName("Acceptar crea la tasca, assigna la incidència, rebutja la resta i avisa els seus professionals")
	void acceptar() throws Exception {
		UUID altre = usuari("PROFESSIONAL");
		String triat = pressupost(professional, "100€");
		String descartat = pressupost(altre, "120€");

		String tasca = cos(acceptar(client, triat)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.estat").value("En curs"))
			.andExpect(jsonPath("$.budget").value("100€"))
			.andExpect(jsonPath("$.titol").value("Fuita"))).get("id").asText();

		assertThat(estatPublicacio(incidencia)).isEqualTo("ASSIGNADA");
		assertThat(estatPressupost(triat)).isEqualTo("ACCEPTAT");
		assertThat(estatPressupost(descartat)).isEqualTo("REBUTJAT");
		assertThat(jdbc.queryForObject("SELECT rebutjat_automaticament FROM pressupostos WHERE id = ?::uuid", Boolean.class, descartat)).isTrue();
		assertThat(jdbc.queryForObject("SELECT pressupost_id::text FROM tasques WHERE id = ?::uuid", String.class, tasca)).isEqualTo(triat);
		assertThat(avisosDe(professional)).isEqualTo(1);
		assertThat(avisosDe(altre)).isEqualTo(1);
	}

	@Test
	@DisplayName("Només el client de la incidència pot acceptar, i cal haver iniciat sessió")
	void acceptarPermisos() throws Exception {
		String id = pressupost(professional, "100€");

		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/pressupostos/" + id + "/acceptar"))
			.andExpect(status().isUnauthorized());
		acceptar(usuari("CLIENT"), id).andExpect(status().isForbidden());
		acceptar(professional, id).andExpect(status().isForbidden());
		acceptar(admin, id).andExpect(status().isForbidden());

		assertThat(estatPressupost(id)).isEqualTo("PENDENT");
		assertThat(files("tasques")).isZero();
	}

	@Test
	@DisplayName("Si falla la creació de la tasca, acceptar no deixa cap canvi a mitges")
	void acceptarEsTransaccional() throws Exception {
		String triat = pressupost(professional, "100€");
		String altre = pressupost(usuari("PROFESSIONAL"), "120€");
		jdbc.execute("CREATE FUNCTION falla_tasca() RETURNS trigger AS $$ BEGIN RAISE EXCEPTION 'fallada provocada'; END; $$ LANGUAGE plpgsql");
		jdbc.execute("CREATE TRIGGER falla_tasca BEFORE INSERT ON tasques FOR EACH ROW EXECUTE FUNCTION falla_tasca()");
		try {
			acceptar(client, triat).andExpect(status().isInternalServerError());
		} finally {
			jdbc.execute("DROP TRIGGER falla_tasca ON tasques");
			jdbc.execute("DROP FUNCTION falla_tasca()");
		}

		assertThat(estatPublicacio(incidencia)).isEqualTo("OBERTA");
		assertThat(estatPressupost(triat)).isEqualTo("PENDENT");
		assertThat(estatPressupost(altre)).isEqualTo("PENDENT");
		assertThat(files("tasques")).isZero();
		assertThat(avisosDe(professional)).isZero();
	}

	@Test
	@DisplayName("Dues acceptacions simultànies només creen una tasca")
	void acceptacioSimultania() throws Exception {
		String primer = pressupost(professional, "100€");
		String segon = pressupost(usuari("PROFESSIONAL"), "120€");
		List<Callable<Integer>> peticions = List.of(
			() -> acceptar(client, primer).andReturn().getResponse().getStatus(),
			() -> acceptar(client, segon).andReturn().getResponse().getStatus()
		);

		assertThat(enParalel(peticions)).containsExactlyInAnyOrder(200, 409);

		assertThat(files("tasques")).isEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pressupostos WHERE estat = 'ACCEPTAT'", Integer.class)).isEqualTo(1);
	}

	@Test
	@DisplayName("Rebutjar el marca sense esborrar-lo, avisa el professional i no es pot acceptar després")
	void rebutjar() throws Exception {
		String id = pressupost(professional, "100€");

		mvc.perform(postCom(usuari("CLIENT"), "/api/pressupostos/" + id + "/rebutjar", "{}")).andExpect(status().isForbidden());
		mvc.perform(postCom(client, "/api/pressupostos/" + id + "/rebutjar", "{}")).andExpect(status().isNoContent());

		assertThat(estatPressupost(id)).isEqualTo("REBUTJAT");
		assertThat(jdbc.queryForObject("SELECT rebutjat_automaticament FROM pressupostos WHERE id = ?::uuid", Boolean.class, id)).isFalse();
		assertThat(avisosDe(professional)).isEqualTo(1);
		acceptar(client, id).andExpect(status().isConflict());
		mvc.perform(get("/api/pressupostos/incidencia/" + incidencia).header("X-User-Rol", "admin"))
			.andExpect(jsonPath("$.length()").value(0));
		mvc.perform(get("/api/pressupostos/professional/" + professional).header("X-User-Id", professional.toString()))
			.andExpect(jsonPath("$[0].estat").value("Rebutjat"));
	}

	private org.springframework.test.web.servlet.ResultActions acceptar(UUID usuari, String pressupost) throws Exception {
		return mvc.perform(postCom(usuari, "/api/pressupostos/" + pressupost + "/acceptar", "{}"));
	}

	private String pressupost(UUID pro, String preu) throws Exception {
		return cos(mvc.perform(postCom(pro, "/api/pressupostos", cosPressupost(preu))).andExpect(status().isCreated())).get("id").asText();
	}

	private String cosPressupost(String preu) {
		return "{\"incidenciaId\":\"" + incidencia + "\",\"preu\":\"" + preu + "\",\"descripcio\":\"d\"}";
	}

	private String estatPressupost(String id) {
		return jdbc.queryForObject("SELECT estat FROM pressupostos WHERE id = ?::uuid", String.class, id);
	}

	private <T> List<T> enParalel(List<Callable<T>> peticions) throws Exception {
		ExecutorService fils = Executors.newFixedThreadPool(peticions.size());
		CountDownLatch sortida = new CountDownLatch(1);
		try {
			List<Future<T>> futurs = new ArrayList<>();
			for (Callable<T> peticio : peticions) {
				futurs.add(fils.submit(() -> {
					sortida.await();
					return peticio.call();
				}));
			}
			sortida.countDown();
			List<T> resultats = new ArrayList<>();
			for (Future<T> futur : futurs) {
				resultats.add(futur.get());
			}
			return resultats;
		} finally {
			fils.shutdownNow();
		}
	}
}
