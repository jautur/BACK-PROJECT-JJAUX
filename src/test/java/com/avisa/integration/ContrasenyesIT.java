package com.avisa.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Les contrasenyes es guarden amb BCrypt: la migració converteix les antigues i la base no n'accepta cap en clar.
 */
class ContrasenyesIT extends MercatIntegrationTest {

	@Autowired
	private PasswordEncoder passwordEncoder;

	private String contrasenyaDe(String email) {
		return jdbc.queryForObject("SELECT password FROM usuaris WHERE LOWER(email) = LOWER(?)", String.class, email);
	}

	@Test
	@DisplayName("La migració V2 converteix les contrasenyes en clar i després la base no n'accepta cap més")
	void migracioConverteixIBloqueja() throws SQLException {
		String esquema = PostgresItSupport.newSchemaName();
		try {
			Flyway.configure().dataSource(PostgresItSupport.url(), PostgresItSupport.user(), PostgresItSupport.password())
				.schemas(esquema).locations("classpath:db/migration").target("1").load().migrate();
			DriverManagerDataSource ds = new DriverManagerDataSource(
				PostgresItSupport.url() + (PostgresItSupport.url().contains("?") ? "&" : "?") + "currentSchema=" + esquema,
				PostgresItSupport.user(), PostgresItSupport.password());
			JdbcTemplate antic = new JdbcTemplate(ds);
			antic.update("INSERT INTO usuaris (nom, email, password, rol) VALUES ('Antic', 'antic@test.es', 'clauAntiga', 'CLIENT')");

			Flyway.configure().dataSource(PostgresItSupport.url(), PostgresItSupport.user(), PostgresItSupport.password())
				.schemas(esquema).locations("classpath:db/migration").load().migrate();

			String hash = antic.queryForObject("SELECT password FROM usuaris WHERE email = 'antic@test.es'", String.class);
			assertThat(hash).startsWith("$2").isNotEqualTo("clauAntiga");
			assertThat(passwordEncoder.matches("clauAntiga", hash)).isTrue();
			assertThatThrownBy(() -> antic.update("INSERT INTO usuaris (nom, email, password, rol) VALUES ('Nou', 'nou@test.es', 'enclar', 'CLIENT')"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("usuaris_password_bcrypt");
		} finally {
			PostgresItSupport.dropSchema(esquema);
		}
	}

	@Test
	@DisplayName("El registre guarda un hash, el login funciona amb la contrasenya i no amb el hash")
	void registreILogin() throws Exception {
		mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(
			"{\"nom\":\"Nova\",\"email\":\"nova@test.es\",\"password\":\"clauNova1\",\"rol\":\"client\",\"telefon\":null,\"acceptedTerms\":true,\"acceptedPrivacy\":true}"))
			.andExpect(status().isOk());

		String hash = contrasenyaDe("nova@test.es");
		assertThat(hash).startsWith("$2").doesNotContain("clauNova1");
		assertThat(passwordEncoder.matches("clauNova1", hash)).isTrue();

		mvc.perform(login("nova@test.es", "clauNova1")).andExpect(status().isOk());
		mvc.perform(login("nova@test.es", "dolenta")).andExpect(status().isUnauthorized());
		mvc.perform(login("nova@test.es", hash)).andExpect(status().isUnauthorized());
		mvc.perform(login("ningu@test.es", "clauNova1")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("Canviar la contrasenya en guarda el hash, i l'anterior deixa de valer")
	void canviDeContrasenya() throws Exception {
		mvc.perform(postCom(client, "/api/auth/canviar-contrasenya", "{\"userId\":\"" + client + "\",\"newPassword\":\"clauNova2\"}"))
			.andExpect(status().isOk());

		String email = client + "@test.es";
		assertThat(contrasenyaDe(email)).startsWith("$2").doesNotContain("clauNova2");
		mvc.perform(login(email, "clauNova2")).andExpect(status().isOk());
		mvc.perform(login(email, ContrasenyaDeProva.CLAR)).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("Els usuaris de prova inserits amb hash poden iniciar sessió")
	void usuarisDeProva() throws Exception {
		UUID altre = usuari("PROFESSIONAL");
		mvc.perform(login(altre + "@test.es", ContrasenyaDeProva.CLAR)).andExpect(status().isOk());
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder login(String email, String password) {
		return post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("{\"email\":\"" + email + "\",\"password\":\"" + password.replace("\\", "\\\\") + "\"}");
	}
}
