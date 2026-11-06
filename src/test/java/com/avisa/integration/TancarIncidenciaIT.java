package com.avisa.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.ResultActions;

class TancarIncidenciaIT extends MercatIntegrationTest {

	private String incidencia;
	private String pressupost;
	private UUID altrePro;

	@BeforeEach
	void incidenciaAmbPressupostos() throws Exception {
		altrePro = usuari("PROFESSIONAL");
		incidencia = cos(mvc.perform(postCom(client, "/api/incidencies", "{\"titol\":\"Fuita\"}"))).get("id").asText();
		pressupost = pressupost(professional);
		pressupost(altrePro);
		jdbc.update("DELETE FROM avisos");
	}

	@Test
	@DisplayName("El client tanca la seua incidència: passa a tancada i els pressupostos pendents es rebutgen avisant els professionals")
	void tancar() throws Exception {
		tancar(client).andExpect(status().isOk()).andExpect(jsonPath("$.estat").value("Tancada"));

		assertThat(estatPublicacio(incidencia)).isEqualTo("TANCADA");
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM pressupostos WHERE estat = 'REBUTJAT' AND NOT rebutjat_automaticament", Integer.class)).isEqualTo(2);
		assertThat(avisosDe(professional)).isEqualTo(1);
		assertThat(avisosDe(altrePro)).isEqualTo(1);
		assertThat(files("tasques")).isZero();
	}

	@Test
	@DisplayName("Només el client de la incidència o un admin la tanquen, i cal sessió")
	void permisos() throws Exception {
		mvc.perform(post("/api/incidencies/" + incidencia + "/tancar")).andExpect(status().isUnauthorized());
		tancar(usuari("CLIENT")).andExpect(status().isForbidden());
		tancar(professional).andExpect(status().isForbidden());
		assertThat(estatPublicacio(incidencia)).isEqualTo("OBERTA");

		tancar(admin).andExpect(status().isOk());
		mvc.perform(postCom(client, "/api/incidencies/" + UUID.randomUUID() + "/tancar", "{}")).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Una incidència ja tancada o assignada no es pot tancar, i una tancada no admet pressupostos")
	void estatsIncorrectes() throws Exception {
		tancar(client).andExpect(status().isOk());
		tancar(client).andExpect(status().isConflict());
		mvc.perform(postCom(usuari("PROFESSIONAL"), "/api/pressupostos", "{\"incidenciaId\":\"" + incidencia + "\",\"preu\":\"1\"}"))
			.andExpect(status().isConflict());

		String altra = cos(mvc.perform(postCom(client, "/api/incidencies", "{\"titol\":\"Altra\"}"))).get("id").asText();
		String p = cos(mvc.perform(postCom(professional, "/api/pressupostos", "{\"incidenciaId\":\"" + altra + "\",\"preu\":\"1\"}"))).get("id").asText();
		mvc.perform(postCom(client, "/api/pressupostos/" + p + "/acceptar", "{}")).andExpect(status().isOk());
		mvc.perform(postCom(client, "/api/incidencies/" + altra + "/tancar", "{}")).andExpect(status().isConflict());
		assertThat(estatPublicacio(altra)).isEqualTo("ASSIGNADA");
	}

	@Test
	@DisplayName("Tancar i acceptar un pressupost alhora no deixen una tasca sobre una incidència tancada")
	void tancarIAcceptarAlhora() throws Exception {
		ExecutorService fils = Executors.newFixedThreadPool(2);
		CountDownLatch sortida = new CountDownLatch(1);
		try {
			List<Callable<Integer>> peticions = List.of(
				() -> tancar(client).andReturn().getResponse().getStatus(),
				() -> mvc.perform(postCom(client, "/api/pressupostos/" + pressupost + "/acceptar", "{}")).andReturn().getResponse().getStatus());
			List<Future<Integer>> futurs = peticions.stream().map(p -> fils.submit(() -> {
				sortida.await();
				return p.call();
			})).toList();
			sortida.countDown();
			for (Future<Integer> futur : futurs) {
				futur.get();
			}
		} finally {
			fils.shutdownNow();
		}

		String estat = estatPublicacio(incidencia);
		assertThat(estat).isIn("TANCADA", "ASSIGNADA");
		assertThat(files("tasques")).isEqualTo("ASSIGNADA".equals(estat) ? 1 : 0);
	}

	@Test
	@DisplayName("El tipus d'un element no depén del prefix del seu identificador")
	void tipusSegonsProfessional() throws Exception {
		mvc.perform(getCom(client, "/api/tasks/" + incidencia))
			.andExpect(jsonPath("$.tipus").value("incidencia")).andExpect(jsonPath("$.estat").value("Oberta"));

		String tasca = cos(mvc.perform(postCom(client, "/api/pressupostos/" + pressupost + "/acceptar", "{}")).andExpect(status().isOk())).get("id").asText();

		mvc.perform(getCom(client, "/api/tasks/" + tasca))
			.andExpect(jsonPath("$.tipus").value("tasca")).andExpect(jsonPath("$.publicacioId").value(incidencia));
		mvc.perform(getCom(admin, "/api/users"))
			.andExpect(jsonPath("$[?(@.id=='" + client + "')].rol").value("CLIENT"));
	}

	private ResultActions tancar(UUID usuari) throws Exception {
		return mvc.perform(postCom(usuari, "/api/incidencies/" + incidencia + "/tancar", "{}"));
	}

	private String pressupost(UUID pro) throws Exception {
		return cos(mvc.perform(postCom(pro, "/api/pressupostos", "{\"incidenciaId\":\"" + incidencia + "\",\"preu\":\"100€\"}")).andExpect(status().isCreated())).get("id").asText();
	}
}
