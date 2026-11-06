package com.avisa.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avisa.imatges.EmmagatzematgeImatges;
import com.avisa.imatges.ServeiImatges;
import java.io.InputStream;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;

/** Imatges sobre PostgreSQL: la restricció de V3 accepta les URLs generades i esborrar un usuari esborra els seus fitxers. */
class ImatgesIT extends MercatIntegrationTest {

	@Autowired
	private EmmagatzematgeImatges magatzem;

	@Test
	@DisplayName("La URL que genera la pujada passa la restricció de la base i surt al perfil")
	void urlAcceptadaPerLaBase() throws Exception {
		String url = pujar(client, "foto");

		assertThat(url).matches("^/api/imatges/[0-9a-f-]{36}\\.(jpg|png)$");
		assertThat(jdbc.queryForObject("SELECT foto_url FROM usuaris WHERE id = ?", String.class, client)).isEqualTo(url);
		mvc.perform(getCom(client, "/api/users/" + client)).andExpect(jsonPath("$.fotoUrl").value(url));
		mvc.perform(get(url)).andExpect(status().isOk());
	}

	@Test
	@DisplayName("Quan l'admin esborra un usuari, els fitxers de la seua foto i el seu logo desapareixen")
	void esborrarUsuariEsborraFitxers() throws Exception {
		String foto = pujar(professional, "foto");
		String logo = pujar(professional, "logo");

		mvc.perform(deleteCom(admin, "/api/users/" + professional)).andExpect(status().isNoContent());

		for (String url : new String[] {foto, ServeiImatges.urlPetita(foto), logo, ServeiImatges.urlPetita(logo)}) {
			assertThat(magatzem.llegir(url.substring(ServeiImatges.PREFIX_URL.length()))).as(url).isEmpty();
		}
	}

	private String pujar(UUID usuari, String tipus) throws Exception {
		byte[] webp;
		try (InputStream in = ImatgesIT.class.getResourceAsStream("/imatges/girada-6.webp")) {
			webp = in.readAllBytes();
		}
		return cos(mvc.perform(multipart("/api/users/" + usuari + "/" + tipus)
				.file(new MockMultipartFile("fitxer", "f.webp", "image/webp", webp))
				.header(HttpHeaders.AUTHORIZATION, bearer(usuari)))
			.andExpect(status().isOk()))
			.get("url").asText();
	}
}
