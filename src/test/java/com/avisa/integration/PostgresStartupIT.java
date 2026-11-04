package com.avisa.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.avisa.AvisaApplication;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Arrenca l'aplicació sencera amb Flyway activat sobre PostgreSQL, com en execució real.
 * La migració V3 queda fora fins que el nou esquema permeta aplicar-la sobre una base buida.
 */
@SpringBootTest(classes = AvisaApplication.class)
class PostgresStartupIT {

	private static final String SCHEMA = PostgresItSupport.newSchemaName();

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@DynamicPropertySource
	static void postgres(DynamicPropertyRegistry registry) {
		String url = PostgresItSupport.url();
		registry.add("spring.datasource.url", () -> url + (url.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA);
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		registry.add("spring.datasource.username", PostgresItSupport::user);
		registry.add("spring.datasource.password", PostgresItSupport::password);
		registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
		registry.add("spring.flyway.enabled", () -> "true");
		registry.add("spring.flyway.schemas", () -> SCHEMA);
		registry.add("spring.flyway.target", () -> "2");
	}

	@AfterAll
	static void dropSchema() throws SQLException {
		PostgresItSupport.dropSchema(SCHEMA);
	}

	@Test
	@DisplayName("L'aplicació arrenca amb Flyway i crea el seu esquema a PostgreSQL")
	void arrencaSobrePostgres() {
		Integer failedMigrations = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM flyway_schema_history WHERE success = FALSE", Integer.class);
		Integer blacklistTable = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = 'blacklist_telefons'",
			Integer.class, SCHEMA);

		assertThat(failedMigrations).isZero();
		assertThat(blacklistTable).isEqualTo(1);
	}
}
