package com.avisa.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Qui pot veure què d'un usuari, i que la identitat surt del token i no de res que envie el client.
 */
class PerfilPublicIT extends MercatIntegrationTest {

	private static final String TELEFON = "600111222";

	private UUID altreClient;

	@BeforeEach
	void dades() {
		altreClient = usuari("CLIENT");
		jdbc.update("UPDATE usuaris SET telefon = ?", TELEFON);
	}

	private String correu(UUID usuari) {
		return usuari + "@test.es";
	}

	private void noExposaDadesPrivades(ResultActions resposta) throws Exception {
		resposta.andExpect(status().isOk())
			.andExpect(content().string(not(containsString("@test.es"))))
			.andExpect(content().string(not(containsString(TELEFON))));
	}

	@Test
	@DisplayName("Sense sessió, el directori dona el perfil amb només els camps públics; /api/users demana sessió")
	void perfilSenseSessio() throws Exception {
		mvc.perform(get("/api/users/" + professional)).andExpect(status().isUnauthorized());
		ResultActions resposta = mvc.perform(get("/api/professionals/" + professional));

		noExposaDadesPrivades(resposta);
		resposta.andExpect(jsonPath("$.id").value(professional.toString()))
			.andExpect(jsonPath("$.nom").exists())
			.andExpect(jsonPath("$.rol").value("PROFESSIONAL"))
			.andExpect(jsonPath("$.verificat").value(false))
			.andExpect(jsonPath("$.percentil").doesNotExist())
			.andExpect(jsonPath("$.sollicitudVerificacio").doesNotExist())
			.andExpect(jsonPath("$.token").doesNotExist());
	}

	@Test
	@DisplayName("El llistat complet només és per a l'admin")
	void llistatNomesAdmin() throws Exception {
		mvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
		mvc.perform(getCom(client, "/api/users")).andExpect(status().isForbidden());
		mvc.perform(getCom(admin, "/api/users")).andExpect(status().isOk()).andExpect(content().string(containsString(correu(professional))));
		noExposaDadesPrivades(mvc.perform(get("/api/professionals")));
	}

	@Test
	@DisplayName("Un usuari sense relació no veu el contacte d'un altre")
	void perfilSenseRelacio() throws Exception {
		noExposaDadesPrivades(mvc.perform(getCom(altreClient, "/api/users/" + professional)));
	}

	@Test
	@DisplayName("El mateix usuari i un administrador veuen totes les dades")
	void perfilPropiIAdmin() throws Exception {
		for (UUID qui : new UUID[] {professional, admin}) {
			mvc.perform(getCom(qui, "/api/users/" + professional)).andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(correu(professional)))
				.andExpect(jsonPath("$.telefon").value(TELEFON))
				.andExpect(jsonPath("$.percentil").value(3));
		}
	}

	@Test
	@DisplayName("El contacte només es veu amb una tasca acceptada i no cancel·lada, en les dues direccions")
	void contacteSegonsEstatDeLaTasca() throws Exception {
		UUID oferta = publicacio("OFERTA", professional, "ACTIVA");
		UUID fila = tasca(oferta, client, professional, "SOLLICITADA");

		noExposaDadesPrivades(mvc.perform(getCom(client, "/api/users/" + professional)));
		noExposaDadesPrivades(mvc.perform(getCom(professional, "/api/users/" + client)));

		for (String estat : new String[] {"EN_CURS", "EN_REVISIO", "FINALITZADA"}) {
			jdbc.update("UPDATE tasques SET estat = ?, data_inici = CURRENT_TIMESTAMP, data_finalitzacio = CASE WHEN ? = 'FINALITZADA' THEN CURRENT_TIMESTAMP END WHERE id = ?",
				estat, estat, fila);
			mvc.perform(getCom(client, "/api/users/" + professional)).andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(correu(professional)))
				.andExpect(jsonPath("$.telefon").value(TELEFON))
				.andExpect(jsonPath("$.percentil").doesNotExist());
			mvc.perform(getCom(professional, "/api/users/" + client)).andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(correu(client)));
			noExposaDadesPrivades(mvc.perform(getCom(altreClient, "/api/users/" + professional)));
		}

		jdbc.update("UPDATE tasques SET estat = 'CANCELLADA', data_finalitzacio = NULL WHERE id = ?", fila);
		noExposaDadesPrivades(mvc.perform(getCom(client, "/api/users/" + professional)));
	}

	@Test
	@DisplayName("Un correu no permet trobar un perfil, llevat d'un administrador")
	void cercaPerCorreu() throws Exception {
		mvc.perform(get("/api/users/" + correu(client))).andExpect(status().isUnauthorized());
		mvc.perform(getCom(altreClient, "/api/users/" + correu(client))).andExpect(status().isNotFound());
		mvc.perform(getCom(admin, "/api/users/" + correu(client))).andExpect(status().isOk());
	}

	@Test
	@DisplayName("/api/auth/me retorna l'usuari del token i no el primer de la base")
	void meEsElDelToken() throws Exception {
		mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
		mvc.perform(getCom(professional, "/api/auth/me")).andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(professional.toString()));
	}

	@Test
	@DisplayName("Ningú pot canviar la contrasenya d'un altre, llevat d'un administrador")
	void canviarContrasenya() throws Exception {
		String cos = "{\"userId\":\"" + client + "\",\"newPassword\":\"novaClau1\"}";
		org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder senseSessio =
			org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/canviar-contrasenya")
				.contentType(MediaType.APPLICATION_JSON).content(cos);

		mvc.perform(senseSessio).andExpect(status().isUnauthorized());
		mvc.perform(postCom(altreClient, "/api/auth/canviar-contrasenya", cos)).andExpect(status().isForbidden());
		assertThat(contrasenya(client)).isEqualTo("secreta");

		mvc.perform(postCom(client, "/api/auth/canviar-contrasenya", cos)).andExpect(status().isOk());
		assertThat(contrasenya(client)).isEqualTo("novaClau1");

		mvc.perform(postCom(admin, "/api/auth/canviar-contrasenya", "{\"userId\":\"" + client + "\",\"newPassword\":\"altraClau2\"}"))
			.andExpect(status().isOk());
		assertThat(contrasenya(client)).isEqualTo("altraClau2");
	}

	@Test
	@DisplayName("Les capçaleres X-User-* no donen cap permís")
	void capcaleresNoDonenPermis() throws Exception {
		String foto = "{\"foto\":\"https://foto.jpg\"}";

		mvc.perform(put("/api/users/" + professional + "/foto").header("X-User-Rol", "admin")
			.contentType(MediaType.APPLICATION_JSON).content(foto)).andExpect(status().isUnauthorized());
		mvc.perform(putCom(client, "/api/users/" + professional + "/foto", foto).header("X-User-Rol", "admin"))
			.andExpect(status().isForbidden());
		mvc.perform(putCom(client, "/api/users/" + professional + "/verificar", "{}").header("X-User-Rol", "admin"))
			.andExpect(status().isForbidden());
		mvc.perform(putCom(client, "/api/users/" + professional, "{\"nom\":\"Robat\"}")
			.header("X-User-Rol", "admin").header("X-User-Id", professional.toString()))
			.andExpect(status().isForbidden());

		assertThat(jdbc.queryForObject("SELECT foto FROM usuaris WHERE id = ?", String.class, professional)).isNull();
		assertThat(jdbc.queryForObject("SELECT verificat FROM usuaris WHERE id = ?", Boolean.class, professional)).isFalse();
		assertThat(jdbc.queryForObject("SELECT nom FROM usuaris WHERE id = ?", String.class, professional)).isNotEqualTo("Robat");
	}

	private String contrasenya(UUID usuari) {
		return jdbc.queryForObject("SELECT password FROM usuaris WHERE id = ?", String.class, usuari);
	}
}
