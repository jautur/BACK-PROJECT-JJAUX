package com.avisa.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.avisa.config.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.List;
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
		jdbc.update("INSERT INTO usuaris (id, nom, email, password, rol) VALUES (?, ?, ?, ?, ?)",
			id, "Usuari " + id, id + "@test.es", ContrasenyaDeProva.HASH, rol);
		return id;
	}

	protected UUID publicacio(String tipus, UUID autor, String estat) {
		UUID id = UUID.randomUUID();
		jdbc.update("INSERT INTO publicacions (id, tipus, autor_id, titol, categoria, preu, estat) VALUES (?, ?, ?, 'Títol', 'Altres Serveis', '10€', ?)",
			id, tipus, autor, estat);
		return id;
	}

	/** Una fila de tasques amb les dates que exigeix el seu estat. */
	protected UUID tasca(UUID publicacio, UUID client, UUID professional, String estat) {
		UUID id = UUID.randomUUID();
		Timestamp ara = new Timestamp(System.currentTimeMillis());
		boolean acceptada = !List.of("SOLLICITADA", "REBUTJADA", "RETIRADA").contains(estat);
		jdbc.update("INSERT INTO tasques (id, publicacio_id, client_id, professional_id, preu_acordat, estat, data_inici, data_finalitzacio) VALUES (?, ?, ?, ?, '10€', ?, ?, ?)",
			id, publicacio, client, professional, estat, acceptada ? ara : null, "FINALITZADA".equals(estat) ? ara : null);
		return id;
	}

	protected MockHttpServletRequestBuilder getCom(UUID usuari, String ruta) {
		return get(ruta).header(HttpHeaders.AUTHORIZATION, bearer(usuari));
	}

	protected MockHttpServletRequestBuilder postCom(UUID usuari, String ruta, String cos) {
		return post(ruta).header(HttpHeaders.AUTHORIZATION, bearer(usuari))
			.contentType(MediaType.APPLICATION_JSON).content(cos);
	}

	protected MockHttpServletRequestBuilder putCom(UUID usuari, String ruta, String cos) {
		return put(ruta).header(HttpHeaders.AUTHORIZATION, bearer(usuari))
			.contentType(MediaType.APPLICATION_JSON).content(cos);
	}

	protected MockHttpServletRequestBuilder deleteCom(UUID usuari, String ruta) {
		return delete(ruta).header(HttpHeaders.AUTHORIZATION, bearer(usuari));
	}

	/** Un token amb el rol real de l'usuari, com el que dona el login. */
	protected String bearer(UUID usuari) {
		String rol = jdbc.queryForObject("SELECT rol FROM usuaris WHERE id = ?", String.class, usuari);
		return "Bearer " + jwtService.generateToken(usuari.toString(), usuari + "@test.es", rol);
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
