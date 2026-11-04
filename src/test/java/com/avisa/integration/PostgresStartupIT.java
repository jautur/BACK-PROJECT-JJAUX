package com.avisa.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Arrenca l'aplicació sencera sobre PostgreSQL buit, com en una instal·lació nova.
 */
class PostgresStartupIT extends PostgresIntegrationTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("L'aplicació arrenca i crea exactament les taules del disseny")
	void arrencaAmbLesTaulesDelDisseny() {
		Integer failedMigrations = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM flyway_schema_history WHERE success = FALSE", Integer.class);
		List<String> tables = jdbcTemplate.queryForList(
			"SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_name <> 'flyway_schema_history'",
			String.class, PostgresItSupport.SHARED_SCHEMA);

		assertThat(failedMigrations).isZero();
		assertThat(tables).containsExactlyInAnyOrder(
			"usuaris", "publicacions", "pressupostos", "tasques", "sollicituds_cancellacio", "avisos", "blacklist_telefons");
	}

	@Test
	@DisplayName("Totes les claus primàries són UUID")
	void lesClausSonUuid() {
		List<String> types = jdbcTemplate.queryForList(
			"SELECT DISTINCT data_type FROM information_schema.columns WHERE table_schema = ? AND column_name = 'id'",
			String.class, PostgresItSupport.SHARED_SCHEMA);

		assertThat(types).containsExactly("uuid");
	}
}
