package com.avisa.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.avisa.config.DemoDataSeeder;
import com.avisa.config.DevUsersSeeder;
import com.avisa.config.JwtService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Regles d'usuaris sobre la pila real: API, servei i PostgreSQL.
 */
@AutoConfigureMockMvc
class UsuarisIT extends PostgresIntegrationTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private JwtService jwtService;

	@BeforeEach
	void baseBuida() {
		jdbc.execute("TRUNCATE usuaris, blacklist_telefons CASCADE");
	}

	private String bearer(UUID usuari, String rol) {
		return "Bearer " + jwtService.generateToken(usuari.toString(), usuari + "@test.es", rol);
	}

	@Test
	@DisplayName("Sense perfil no hi ha seeders")
	void senseProfilNoHiHaSeeders() {
		assertThat(context.getBeansOfType(DevUsersSeeder.class)).isEmpty();
		assertThat(context.getBeansOfType(DemoDataSeeder.class)).isEmpty();
	}

	@Test
	@DisplayName("Un administrador no es pot esborrar, siga quin siga el seu identificador")
	void adminProtegitPerRol() throws Exception {
		UUID admin = usuari("ADMIN", "qualsevol@admin.test", null);
		String token = bearer(admin, "ADMIN");

		mvc.perform(delete("/api/users/" + admin).header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isBadRequest());
		mvc.perform(delete("/api/users/qualsevol@admin.test").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isBadRequest());

		assertThat(existeix(admin)).isTrue();
	}

	@Test
	@DisplayName("Un administrador pot esborrar un usuari sense historial")
	void esborraUsuariSenseHistorial() throws Exception {
		UUID client = usuari("CLIENT", "client@test.es", null);
		String token = bearer(usuari("ADMIN", "admin@admin.test", null), "ADMIN");

		mvc.perform(delete("/api/users/" + client).header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isNoContent());

		assertThat(existeix(client)).isFalse();
	}

	@Test
	@DisplayName("Un usuari amb historial no es pot esborrar")
	void noEsborraUsuariAmbHistorial() throws Exception {
		UUID client = usuari("CLIENT", "client@test.es", null);
		String token = bearer(usuari("ADMIN", "admin@admin.test", null), "ADMIN");
		jdbc.update("INSERT INTO publicacions (tipus, autor_id, titol, categoria, estat) VALUES ('INCIDENCIA', ?, 't', 'c', 'OBERTA')", client);

		mvc.perform(delete("/api/users/" + client).header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isBadRequest());

		assertThat(existeix(client)).isTrue();
	}

	@Test
	@DisplayName("Sense ser administrador no es pot esborrar cap usuari, ni el propi compte")
	void nomesAdminEsborra() throws Exception {
		UUID client = usuari("CLIENT", "client@test.es", null);
		UUID altre = usuari("CLIENT", "altre@test.es", null);

		mvc.perform(delete("/api/users/" + client)).andExpect(status().isUnauthorized());
		mvc.perform(delete("/api/users/" + client).header(HttpHeaders.AUTHORIZATION, bearer(altre, "CLIENT"))).andExpect(status().isForbidden());
		mvc.perform(delete("/api/users/" + client).header(HttpHeaders.AUTHORIZATION, bearer(client, "CLIENT"))).andExpect(status().isForbidden());

		assertThat(existeix(client)).isTrue();
	}

	@Test
	@DisplayName("El registre rebutja rols fora de client i professional")
	void registreRebutjaRols() throws Exception {
		for (String rol : new String[] {"admin", "ADMIN", "superhacker"}) {
			mvc.perform(registre("x@test.es", rol, null)).andExpect(status().isBadRequest());
		}

		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuaris", Integer.class)).isZero();
	}

	@Test
	@DisplayName("El registre guarda el rol en majúscules i l'API el retorna igual")
	void registreDeClient() throws Exception {
		mvc.perform(registre("nova@test.es", "client", "600000001"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.rol").value("CLIENT"));

		assertThat(jdbc.queryForObject("SELECT rol FROM usuaris WHERE email = 'nova@test.es'", String.class)).isEqualTo("CLIENT");
		mvc.perform(registre("NOVA@test.es", "client", null)).andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("El registre rebutja un telèfon de la llista negra, encara que tinga un altre format")
	void registreAmbTelefonBloquejat() throws Exception {
		bloquejar("600123456");

		mvc.perform(registre("bloquejat@test.es", "professional", "+34 600 123 456")).andExpect(status().isBadRequest());

		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuaris", Integer.class)).isZero();
	}

	@Test
	@DisplayName("El perfil rebutja canviar a un telèfon de la llista negra")
	void perfilAmbTelefonBloquejat() throws Exception {
		UUID client = usuari("CLIENT", "client@test.es", "600000002");
		bloquejar("600123456");

		mvc.perform(perfil(client, "600-123-456")).andExpect(status().isBadRequest());

		assertThat(jdbc.queryForObject("SELECT telefon FROM usuaris WHERE id = ?", String.class, client)).isEqualTo("600000002");
	}

	@Test
	@DisplayName("Un usuari ja bloquejat pot editar el perfil si no canvia de telèfon")
	void perfilSenseCanviarTelefon() throws Exception {
		UUID client = usuari("CLIENT", "client@test.es", "600123456");
		bloquejar("600123456");

		mvc.perform(perfil(client, "600 123 456")).andExpect(status().isOk());
	}

	@Test
	@DisplayName("La sol·licitud de verificació avisa tots els administradors")
	void verificacioAvisaAdmins() throws Exception {
		usuari("ADMIN", "a1@admin.test", null);
		usuari("ADMIN", "a2@admin.test", null);
		UUID professional = usuari("PROFESSIONAL", "pro@test.es", "600000003");

		mvc.perform(post("/api/users/" + professional + "/sollicitar-verificacio").header(HttpHeaders.AUTHORIZATION, bearer(professional, "PROFESSIONAL")))
			.andExpect(status().isOk());

		assertThat(jdbc.queryForObject(
			"SELECT COUNT(DISTINCT a.destinatari_id) FROM avisos a JOIN usuaris u ON u.id = a.destinatari_id WHERE u.rol = 'ADMIN'",
			Integer.class)).isEqualTo(2);
	}

	@Test
	@DisplayName("Un avís per a un destinatari inexistent dona error en lloc de fingir èxit")
	void avisSenseDestinatari() throws Exception {
		mvc.perform(post("/api/avisos").header(HttpHeaders.AUTHORIZATION, bearer(usuari("ADMIN", "admin@admin.test", null), "ADMIN")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"destinatariId\":\"" + UUID.randomUUID() + "\",\"missatge\":\"hola\"}"))
			.andExpect(status().isBadRequest());

		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM avisos", Integer.class)).isZero();
	}

	@Test
	@DisplayName("Si no es pot consultar la llista negra, el registre falla en lloc de deixar passar el telèfon")
	void llistaNegraInaccessible() throws Exception {
		jdbc.execute("ALTER TABLE blacklist_telefons RENAME TO blacklist_amagada");
		try {
			mvc.perform(registre("nou@test.es", "client", "600999888")).andExpect(status().isInternalServerError());
		} finally {
			jdbc.execute("ALTER TABLE blacklist_amagada RENAME TO blacklist_telefons");
		}

		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuaris", Integer.class)).isZero();
	}

	@Test
	@DisplayName("Si la base falla, el login i el llistat d'usuaris donen error 500 en lloc de fer-se passar per credencials incorrectes o per una llista buida")
	void errorDeBaseNoEsConfonCambCredencialsIncorrectes() throws Exception {
		usuari("CLIENT", "cli@test.es", null);
		jdbc.execute("ALTER TABLE usuaris RENAME TO usuaris_amagada");
		try {
			mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"cli@test.es\",\"password\":\"secreta\"}")).andExpect(status().isInternalServerError());
			mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(UUID.randomUUID(), "ADMIN"))).andExpect(status().isInternalServerError());
		} finally {
			jdbc.execute("ALTER TABLE usuaris_amagada RENAME TO usuaris");
		}
	}

	private UUID usuari(String rol, String email, String telefon) {
		UUID id = UUID.randomUUID();
		jdbc.update("INSERT INTO usuaris (id, nom, email, password, rol, telefon) VALUES (?, ?, ?, 'secreta', ?, ?)",
			id, "Usuari " + email, email, rol, telefon);
		return id;
	}

	private void bloquejar(String telefon) {
		jdbc.update("INSERT INTO blacklist_telefons (telefon) VALUES (?)", telefon);
	}

	private boolean existeix(UUID id) {
		Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM usuaris WHERE id = ?", Integer.class, id);
		return count != null && count > 0;
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder registre(String email, String rol, String telefon) {
		String tel = telefon == null ? "null" : "\"" + telefon + "\"";
		return post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(
			"{\"nom\":\"Prova\",\"email\":\"" + email + "\",\"password\":\"123456\",\"rol\":\"" + rol + "\",\"telefon\":" + tel
				+ ",\"acceptedTerms\":true,\"acceptedPrivacy\":true}");
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder perfil(UUID id, String telefon) {
		return put("/api/users/" + id).header(HttpHeaders.AUTHORIZATION, bearer(id, "CLIENT")).contentType(MediaType.APPLICATION_JSON)
			.content("{\"nom\":\"Editat\",\"telefon\":\"" + telefon + "\",\"bio\":\"bio\"}");
	}
}
