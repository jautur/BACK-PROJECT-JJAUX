package com.avisa.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.avisa.config.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Base dels tests del mercat: passen per l'API, amb tokens JWT reals, fins a PostgreSQL.
 */
@AutoConfigureMockMvc
abstract class MercatIntegrationTest extends PostgresIntegrationTest {

	@Autowired
	protected MockMvc mvc;

	@Autowired
	protected JdbcTemplate jdbc;

	@Autowired
	private JwtService jwtService;

	private final ObjectMapper json = new ObjectMapper();

	protected UUID client;
	protected UUID professional;
	protected UUID admin;

	@BeforeEach
	void baseDelMercat() {
		jdbc.execute("TRUNCATE usuaris, blacklist_telefons CASCADE");
		client = usuari("CLIENT");
		professional = usuari("PROFESSIONAL");
		admin = usuari("ADMIN");
	}

	protected UUID usuari(String rol) {
		UUID id = UUID.randomUUID();
		jdbc.update("INSERT INTO usuaris (id, nom, email, password, rol) VALUES (?, ?, ?, 'secreta', ?)",
			id, "Usuari " + id, id + "@test.es", rol);
		return id;
	}

	protected MockHttpServletRequestBuilder postCom(UUID usuari, String ruta, String cos) {
		return post(ruta).header(HttpHeaders.AUTHORIZATION, bearer(usuari))
			.contentType(MediaType.APPLICATION_JSON).content(cos);
	}

	protected MockHttpServletRequestBuilder deleteCom(UUID usuari, String ruta) {
		return delete(ruta).header(HttpHeaders.AUTHORIZATION, bearer(usuari));
	}

	private String bearer(UUID usuari) {
		return "Bearer " + jwtService.generateToken(usuari.toString(), usuari + "@test.es", "client");
	}

	protected JsonNode cos(org.springframework.test.web.servlet.ResultActions resposta) throws Exception {
		return json.readTree(resposta.andReturn().getResponse().getContentAsString());
	}

	protected String estatPublicacio(String id) {
		return jdbc.queryForObject("SELECT estat FROM publicacions WHERE id = ?::uuid", String.class, id);
	}

	protected int avisosDe(UUID usuari) {
		Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM avisos WHERE destinatari_id = ?", Integer.class, usuari);
		return total == null ? 0 : total;
	}

	protected int files(String taula) {
		Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM " + taula, Integer.class);
		return total == null ? 0 : total;
	}
}
