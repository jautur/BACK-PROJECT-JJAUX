package com.avisa.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Comprova que les migracions reals s'apliquen sobre un PostgreSQL buit.
 */
class FlywayMigrationsIT {

	private final String schema = PostgresItSupport.newSchemaName();

	@AfterEach
	void dropSchema() throws SQLException {
		PostgresItSupport.dropSchema(schema);
	}

	@Test
	@DisplayName("Totes les migracions s'apliquen sobre un esquema buit")
	void aplicaTotesLesMigracions() {
		MigrateResult result = Flyway.configure()
			.dataSource(PostgresItSupport.url(), PostgresItSupport.user(), PostgresItSupport.password())
			.schemas(schema)
			.locations("classpath:db/migration")
			.load()
			.migrate();

		assertThat(result.success).isTrue();
		assertThat(result.migrationsExecuted).isPositive();
	}
}
